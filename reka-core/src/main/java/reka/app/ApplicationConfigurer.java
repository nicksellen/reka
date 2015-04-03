package reka.app;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static reka.api.Path.path;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.allExceptionMessages;
import static reka.util.Util.runtime;
import static reka.util.Util.safelyCompletable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.TestConfigurer;
import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.config.DefaultConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.data.Data;
import reka.data.DiffContentConsumer.DiffContentType;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.flow.FlowSegment;
import reka.flow.FlowTest;
import reka.flow.FlowTest.FlowTestCase;
import reka.flow.builder.FlowBuilderGroup;
import reka.flow.builder.FlowVisualizer;
import reka.flow.builder.Flows;
import reka.flow.ops.Subscriber;
import reka.module.ModuleManager;
import reka.module.RootModule;
import reka.module.setup.AppSetup.ApplicationCheck;
import reka.module.setup.ApplicationSetup;
import reka.module.setup.ModuleConfigurer;
import reka.module.setup.Trigger;
import reka.module.setup.TriggerFlows;
import reka.util.Identity;
import reka.util.dirs.AppDirs;

public class ApplicationConfigurer implements ErrorReporter {
	
	private static final ExecutorService executor = Executors.newCachedThreadPool(); // just used for app configure/deployments
	
	private static final Logger log = LoggerFactory.getLogger(ApplicationConfigurer.class);
    private Path applicationName;
	
	private final ModuleManager modules;

    private final List<Config> defs = new ArrayList<>();
    private final List<Config> testConfigs = new ArrayList<>();
    
    private final ModuleConfigurer rootModule;
    
    private final MutableData meta = MutableMemoryData.create();
    
    public ApplicationConfigurer(AppDirs dirs, ModuleManager modules) {
    	this.modules = modules;
        rootModule = new RootModule(dirs, modules.modules());
    }
    
    @Conf.At("name")
    public void name(String val) {
        applicationName = slashes(val);
    }
    
    @Conf.At("meta")
    public void meta(Config val) {
    	checkConfig(val.hasBody(), "must have a body");
    	meta.merge(configToData(val.body()));
    }
    
    @Conf.EachUnmatched
    public void use(Config config) {
    	log.info("setting up module {}{}", config.key(), config.hasValue() ? " " + config.valueAsString() : "");
    	rootModule.useThisConfig(config);
    }
    
    @Conf.Each("def")
    @Conf.Each("on")
    public void def(Config config) {
    	checkConfig(config.hasValue(), "you must provide a value/name");
    	defs.add(config);
    }
    
    @Conf.Each("test")
    public void test(Config config) {
    	checkConfig(config.hasValue(), "you must provide a value/name");
    	testConfigs.add(config);
    }

	@Override
	public void errors(ErrorCollector errors) {
		if (applicationName == null) errors.add("name is required");
	}
    
	public Set<Path> modulePaths() {
		return rootModule.modulePaths();
	}
	
    public Path name() {
        return applicationName;
    }
    
    public Collection<FlowVisualizer> visualize(IdentityAndVersion idv) {
        FlowBuilderGroup flowsBuilder = new FlowBuilderGroup();
    	ApplicationSetup initializer = ModuleConfigurer.setup(idv, rootModule, new HashMap<>()); // TODO: does this need stores for the modules?
    	
    	DefaultConfigurerProvider provider = new DefaultConfigurerProvider(initializer.providers);
    	Map<Path,Supplier<FlowSegment>> configuredFlows = new HashMap<>();
    	defs.forEach((config) -> 
			configuredFlows.put(path(config.valueAsString()), 
					configure(new SequenceConfigurer(provider), config).bind()));
    	configuredFlows.forEach((name, segment) -> flowsBuilder.add(name, segment.get()));
    	
    	return flowsBuilder.buildVisualizers();
    }
    
    public void checkValid(IdentityAndVersion idv, Map<Path,IdentityStore> stores) {
    	ApplicationSetup setup = ModuleConfigurer.setup(idv, rootModule, stores);
    	DefaultConfigurerProvider configurerProvider = new DefaultConfigurerProvider(setup.providers);
    	setup.triggers.forEach(triggers -> triggers.get().forEach(trigger -> {
    		trigger.supplier().apply(configurerProvider).bind(trigger.base(), triggers.ctx()).get();
    	}));
    	defs.forEach(config -> {
    		configure(new SequenceConfigurer(configurerProvider), config).bind().get();
    	});
    	testConfigs.forEach(config -> {
    		configure(new TestConfigurer(configurerProvider), config).build();
    	});
    	runChecks(idv.identity(), setup);
    }
    
    private void runChecks(Identity identity, ApplicationSetup setup) {
    	List<String> checkErrors = new ArrayList<>();
    	setup.checks.forEach(check -> {
    		ApplicationCheck appCheck = new ApplicationCheck(identity);
    		check.accept(appCheck);
    		appCheck.errors().forEach(error -> {
    			checkErrors.add(error);
    		});
    	});
    	if (!checkErrors.isEmpty()) {
    		throw new RuntimeException(checkErrors.stream().collect(joining(", ")));
    	}
    }
    
    private void runPortCheckers(Identity identity, ApplicationSetup setup) {
    	Set<String> errors = new HashSet<>();
    	modules.portCheckers().forEach(checker -> {
			setup.networkRequirements.forEach(req -> {
				if (!checker.check(identity, req.port(), req.host())) {
					if (req.host().isPresent()) {
						errors.add(format("%s:%d is not available", req.host().get(), req.port()));
					} else {
						errors.add(format("%d is not available", req.port()));
					}
				}
			});
    	});
    	if (!errors.isEmpty()) throw runtime(errors.stream().collect(joining(", ")));
    }
    
    private static Path triggerPath(Trigger trigger) {
    	return trigger.base().add(trigger.key().name());
    }
    
    public CompletableFuture<Application> build(Identity identity, int applicationVersion, Map<Path,IdentityStore> stores) {
    	return safelyCompletable(future -> {
    		
    		FlowBuilderGroup initflowBuilders = new FlowBuilderGroup();
    		FlowBuilderGroup flowBuilders = new FlowBuilderGroup();
    		    		
	    	ApplicationSetup setup = ModuleConfigurer.setup(IdentityAndVersion.create(identity, applicationVersion), rootModule, stores);
	    	
	    	runChecks(identity, setup);
	    	runPortCheckers(identity, setup);
	    	
	    	stores.forEach((path, store) -> {
	    		setup.registerStore(path, store);
	    	});
	    	
	    	setup.identity(identity);
	    	setup.name(applicationName);
	    	setup.meta(meta.immutable());
	    	setup.version(applicationVersion);
	    	
	    	Map<Path,FlowTest> tests = new HashMap<>();
	    	
	    	DefaultConfigurerProvider configurerProvider = new DefaultConfigurerProvider(setup.providers);
	    	
	    	setup.initflows.forEach(initflow -> {
	    		initflowBuilders.add(initflow.name, 
    					initflow.supplier.apply(configurerProvider).bind(initflow.name, initflow.ctx).get());
	    	});
	    	
	    	setup.triggers.forEach(triggers -> {
	    		triggers.get().forEach(trigger -> {
	    			flowBuilders.add(triggerPath(trigger), 
	    					trigger.supplier().apply(configurerProvider).bind(trigger.base(), triggers.ctx()).get());
	    		});
	    	});
	    	
	    	defs.forEach(config -> 
				flowBuilders.add(path(config.valueAsString()), 
						configure(new SequenceConfigurer(configurerProvider), config).bind().get()));
	    	
	    	AtomicInteger num = new AtomicInteger();
	    	testConfigs.forEach(config -> {
	    		Path testName = path("test").add(config.hasValue() ? config.valueAsString() : String.valueOf(num.incrementAndGet()));
	    		FlowTest test = configure(new TestConfigurer(configurerProvider), config).build();
	    		flowBuilders.add(testName, test.run().get());
	    		tests.put(testName, test);
	    	});
	    	
	    	// ok, initialize this thing!
	    	
	    	ApplicationInitializer appi = new ApplicationInitializer(future, identity, flowBuilders, setup, tests);
	    	
	    	log.debug("initializing app");
	    	
	    	setup.initializationFlow.prepare().operationExecutor(executor).mutableData(MutableMemoryData.create()).run(appi);
    	
    	});
    }
    
    private static class ApplicationInitializer implements Subscriber {

    	private final CompletableFuture<Application> future;
    	
    	private final Identity identity;
    	private final FlowBuilderGroup flowBuilders;
    	
    	private final ApplicationSetup setup;
    	private final Map<Path,FlowTest> tests;
    	
    	public ApplicationInitializer(
				CompletableFuture<Application> future, 
				Identity identity,
				FlowBuilderGroup flowBuilders,
				ApplicationSetup setup, 
				Map<Path, FlowTest> tests) {
			this.future = future;
			this.identity = identity;
			this.flowBuilders = flowBuilders;
			this.setup = setup;
			this.tests = tests;
		}
    	
    	private final Logger log = LoggerFactory.getLogger(getClass());
    	
    	@Override
		public void halted() {
    		log.error("halted whilst initializing app :(");
			future.completeExceptionally(runtime("halted whilst initializing app :("));
		}

		@Override
		public void error(Data data, Throwable t) {
			log.error("error whilst initializing app :( {} {}", t.getMessage(), data.toPrettyJson());
			future.completeExceptionally(t);
		}
		
		@Override
		public void ok(MutableData data) {
			log.debug("initialized app");
			
			log.debug("NOT building init flows");

  /*
			
			Flows initFlows = initflowBuilders.build();
			
			initializer.initflows().forEach(initflow -> {
				log.debug("passing build initflow to {}", initflow.name.slashes());
				initflow.consumer.accept(initFlows.flow(initflow.name));
			});
  */
			safelyCompletable(future, () -> {
	    		
	    		log.debug("building main flows");
	    		
				Flows flows = flowBuilders.build();
				
				setup.flows(flows);
				
				runTests(flows, tests, identity).whenComplete((ignored, ex) -> {
					
					if (ex != null) {
						future.completeExceptionally(ex);
						return;
					}
					
					try {
					
						setup.triggers.forEach(triggers -> {
							Map<IdentityKey<Flow>,Flow> m = new HashMap<>();
							triggers.get().forEach(trigger -> {
								m.put(trigger.key(), flows.flow(triggerPath(trigger)));
							});
							triggers.consumer().accept(new TriggerFlows(identity, m));
				    	});
						
						Application app = setup.buildApplication();
						
				    	future.complete(app);
			    	
					} catch (Throwable t) {
						future.completeExceptionally(t);
					}
					
				});
				
			});
		}
    	
    }


	private static CompletableFuture<Void> runTests(Flows flows, Map<Path, FlowTest> tests, Identity identity) {
		
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (tests.isEmpty()) {
			future.complete(null);
			return future;
		}
		
		log.info("running {} tests", tests.size());
		
		AtomicInteger remaining = new AtomicInteger(tests.size());
		
		List<String> testErrors = Collections.synchronizedList(new ArrayList<>());

		ScheduledFuture<?> timeout = Reka.SharedExecutors.scheduled.schedule(() -> {
			future.completeExceptionally(runtime("failed to deploy [%s] because tests timed out", identity.name()));
		}, 10, TimeUnit.SECONDS);
		
		Runnable testsFinished = () -> {
			if (future.isDone()) return; // too late
			timeout.cancel(true);
			if (testErrors.isEmpty()) {
				future.complete(null);
			} else {
				String msg = format("failed to deploy [%s] because tests failed:\n%s", identity.name(), 
						testErrors.stream().map(s -> format("- %s", s)) .collect(joining("\n")));
				log.error(msg);
				future.completeExceptionally(runtime(msg));
			}
		};
		
		tests.forEach((name, test) -> {
			SequentialTestCasesRunner.run(name, flows.flow(name), test.cases(), errors -> {
				testErrors.addAll(errors);
				if (remaining.decrementAndGet() == 0) {
					testsFinished.run();
				}
			});
		});
				
		return future;
	}
	
	private static class SequentialTestCasesRunner {
		
		private final Path name;
		private final Flow flow;
		private final Iterator<FlowTestCase> cases;
		private final List<String> testErrors = new ArrayList<>();
		private final Consumer<List<String>> completion;
		
		private static void run(Path name, Flow flow, List<FlowTestCase> cases, Consumer<List<String>> completion) {
			new SequentialTestCasesRunner(name, flow, cases, completion).runNext();
		}
		
		private SequentialTestCasesRunner(Path name, Flow flow, List<FlowTestCase> cases, Consumer<List<String>> completion) {
			this.name = name;
			this.flow = flow;
			this.cases = cases.iterator();
			this.completion = completion;
		}
		
		private void complete() {
			completion.accept(testErrors);
		}
		
		private void runNext() {
			
			if (!cases.hasNext()) {
				complete();
				return;
			}
			
			FlowTestCase testCase = cases.next();
			
			flow.prepare().operationExecutor(executor).data(testCase.initial()).run(new Subscriber(){
				
				@Override
				public void ok(MutableData data) {
					try {
						data.diffContentFrom(testCase.expect(), (path, type, expected, actual) -> {
							if (type == DiffContentType.ADDED) return; // don't mind extra data for now...
							
							/* something about the initvalue was put in for a reason but I don't know why anymore
							 * something about if the value was not touched then that is ok? *shrug* I'll leave it in for now
							 * Optional<Content> initvalue = testCase.initial().getContent(path);
							if (initvalue.isPresent() && initvalue.get().equals(actual)) {
								// data just stayed as what we put in
								return;
							}
							*/
							
							if (actual != null && expected != null &&
								actual.toString().equals(expected.toString())) {
								// let this go by...
								// TODO: how to handle equals of numeric types?
							} else {
								testErrors.add(format("%s : %s\ncontent at %s %s - expected [%s] got [%s]", name.join(" / "), testCase.name(), path.dots(), type, expected, actual));
							}
							
						});
					} catch (Throwable t) {
						t.printStackTrace();
						testErrors.add(format("%s : %s\nexception during test - %s", name.join(" / "), testCase.name(), allExceptionMessages(t, ", ")));
					} finally {
						runNext();
					}
				}
				
				@Override
				public void halted() {
					log.error("test halted");
					testErrors.add(format("%s : %s\nhalted during test", name.join(" / "), testCase.name()));
					runNext();
				}
				
				@Override
				public void error(Data data, Throwable t) {
					log.error("test failed to run", t);
					testErrors.add(format("%s : %s\nexception during test - %s", name.join(" / "), testCase.name(), allExceptionMessages(t, ", ")));
					runNext();
				}
				
			});
			
		}
	}

}
