package reka;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.safelyCompletable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.ConcurrentIdentityStore;
import reka.api.IdentityKey;
import reka.api.Path;
import reka.api.content.Contents;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.api.run.EverythingSubscriber;
import reka.api.run.Subscriber;
import reka.config.Config;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.FlowBuilders;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.bundle.BundleManager;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleConfigurer.ModuleInitializer;
import reka.core.bundle.ModuleSetup.MultiFlowRegistration;
import reka.core.bundle.ModuleSetup.Trigger;
import reka.core.bundle.ModuleSetup.TriggerCollection;
import reka.core.config.MultiConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.data.memory.MutableMemoryData;

public class ApplicationConfigurer implements ErrorReporter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
    private Path applicationName;
    
    private final MutableData meta = MutableMemoryData.create();
    
    public ApplicationConfigurer(BundleManager bundles) {
        rootModule = new RootModule();
		rootModule.modules(bundles.modules());
    }
    
    private final List<Config> flowConfigs = new ArrayList<>();
    
    private final ModuleConfigurer rootModule;
    
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
    public void useModule(Config config) {
    	log.info("setting up module {} {}", config.key(), config.hasValue() ? config.valueAsString() : "<unnamed>");
    	rootModule.useThisConfig(config);
    }
    
    @Conf.Each("use")
    public void use(Config config) {
    	rootModule.use(config);
    }
    
    @Conf.Each("def")
    public void def(Config config) {
    	checkConfig(config.hasValue(), "you must provide a value/name");
    	flowConfigs.add(config);
    }

	@Override
	public void errors(ErrorCollector errors) {
		if (applicationName == null) errors.add("name is required");
	}
    
    public Path name() {
        return applicationName;
    }
    
    public Collection<FlowVisualizer> visualize() {
        FlowBuilders flowsBuilder = new FlowBuilders();
    	ModuleInitializer initializer = ModuleConfigurer.buildInitializer(rootModule);
    	
    	MultiConfigurerProvider provider = new MultiConfigurerProvider(initializer.providers());
    	Map<Path,Supplier<FlowSegment>> configuredFlows = new HashMap<>();
    	flowConfigs.forEach((config) -> 
			configuredFlows.put(path(config.valueAsString()), 
					configure(new SequenceConfigurer(provider), config)));
		
    	configuredFlows.forEach((name, segment) -> flowsBuilder.add(name, segment.get()));
    	
    	return flowsBuilder.buildVisualizers();
    }

    public CompletableFuture<Application> build(String identity, int version) {
    	return build(identity, version, null);
    }
    
    public CompletableFuture<Application> build(String identity, int version, Application previous) {
    	return build(identity, version, previous, EverythingSubscriber.DO_NOTHING);
    }
    
    public void checkValid() {
    	ModuleInitializer initializer = ModuleConfigurer.buildInitializer(rootModule);
    	MultiConfigurerProvider configurerProvider = new MultiConfigurerProvider(initializer.providers());
    	initializer.triggers().forEach(triggers -> triggers.get().forEach(trigger -> trigger.supplier().apply(configurerProvider).get()));
    	flowConfigs.forEach((config) -> configure(new SequenceConfigurer(configurerProvider), config).get());
    }
    
    public static class TriggerSetup {

    	private final TriggerCollection triggers;
    	
		public TriggerSetup(TriggerCollection triggers) {
			this.triggers = triggers;
		}
		
		public TriggerCollection triggers() {
			return triggers;
		}
    	
    }
    
    private Path triggerPath(Trigger trigger) {
    	return applicationName.add(trigger.base()).add(trigger.key().name());
    }
    
    public CompletableFuture<Application> build(String identity, int applicationVersion, Application previous, final Subscriber s) {

    	CompletableFuture<Application> future = new CompletableFuture<>();
    	
    	return safelyCompletable(future, () -> {
    		
    		FlowBuilders flowBuilders = new FlowBuilders();
    		
	    	ModuleInitializer initializer = ModuleConfigurer.buildInitializer(rootModule);
	    	
	    	ApplicationBuilder applicationBuilder = new ApplicationBuilder();
	    	
	    	applicationBuilder.name(applicationName);
	    	applicationBuilder.meta(meta.immutable());
	    	applicationBuilder.version(applicationVersion);
	    	applicationBuilder.initializerVisualizer(initializer.visualizer());
	    	
	    	List<Runnable> shutdownHandlers = initializer.shutdownHandlers();
	    	
	    	MultiConfigurerProvider configurerProvider = new MultiConfigurerProvider(initializer.providers());
	    	
	    	initializer.triggers().forEach(triggers -> {
	    		triggers.get().forEach(trigger -> {
	    			flowBuilders.add(triggerPath(trigger), trigger.supplier().apply(configurerProvider).get());
	    		});
	    	});
	    	
	    	flowConfigs.forEach((config) -> 
				flowBuilders.add(applicationName.add(config.valueAsString()), 
						configure(new SequenceConfigurer(configurerProvider), config).get()));
	    	
	    	// ok, run the app initializer
	
	    	EverythingSubscriber subscriber = EverythingSubscriber.wrap(s);
	    	
	    	MutableData initdata = MutableMemoryData.create();
	    	
	    	initdata.put(path("store"), Contents.nonSerializableContent(new ConcurrentIdentityStore()));
	    	
	    	initializer.flow().prepare().data(initdata).complete(new EverythingSubscriber() {
				
				@Override
				public void ok(MutableData data) {
					
					log.debug("initialized with [{}]", data.toPrettyJson());
					
			    	try {
			    		
						Flows flows = flowBuilders.build(data);
						
						applicationBuilder.flows(flows);
						
						initializer.triggers().forEach(triggers -> {
							
							Map<IdentityKey<Flow>,Flow> m = new HashMap<>();
							
							triggers.get().forEach(trigger -> {
								m.put(trigger.key(), flows.flow(triggerPath(trigger)));
							});
							
							MultiFlowRegistration mr = new MultiFlowRegistration(applicationVersion, identity, m);
							triggers.consumer().accept(mr);
							
							applicationBuilder.network().addAll(mr.network());
							applicationBuilder.undeployConsumers().addAll(mr.undeployConsumers());
							applicationBuilder.pauseConsumers().addAll(mr.pauseConsumers());
							applicationBuilder.resumeConsumers().addAll(mr.resumeConsumers());
				    	});
						
						applicationBuilder.undeployConsumers().add(version -> {
							for (Runnable handler : shutdownHandlers) {
								try {
									handler.run();
								} catch (Throwable t) {
									t.printStackTrace();
								}
							}
						});
			    		
				    	future.complete(applicationBuilder.build());
				    	
				    	subscriber.ok(data);
			    	
			    	} catch (Throwable t) {
			    		t.printStackTrace();
			    		subscriber.error(data, t);
			    		if (!future.isDone()) {
			    			future.completeExceptionally(t);
			    		}
			    	}
				}
				
				@Override
				public void halted() {
					log.debug("halted whilst initializing app :(");
					subscriber.halted();
					future.cancel(true);
				}
				
				@Override
				public void error(Data data, Throwable t) {
					log.debug("error whilst initializing app :( {} {}", t.getMessage(), data.toPrettyJson());
					t.printStackTrace();
					subscriber.error(data, t);
					future.completeExceptionally(t);
				}
			}).run();
    	
    	});
    }

}
