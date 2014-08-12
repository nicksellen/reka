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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.EverythingSubscriber;
import reka.api.run.Subscriber;
import reka.config.Config;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.builder.FlowsBuilder;
import reka.core.bundle.BundleManager;
import reka.core.bundle.Registration;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseConfigurer.UsesInitializer;
import reka.core.bundle.UseInit.Registration2;
import reka.core.bundle.UseInit.Trigger;
import reka.core.config.MultiConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.data.memory.MutableMemoryData;

public class ApplicationConfigurer implements ErrorReporter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
    private Path applicationName;
    
    private final MutableData meta = MutableMemoryData.create();
    
    public ApplicationConfigurer(BundleManager bundles) {
        rootUse = new UseRoot();
		rootUse.mappings(bundles.uses());
    }

    private final FlowsBuilder flowsBuilder = new FlowsBuilder();
    
    private final List<Config> flowConfigs = new ArrayList<>();
    
    private final UseConfigurer rootUse;
    
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
    	rootUse.useThisConfig(config);
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
    	UsesInitializer initializer = UseConfigurer.process(rootUse);
    	
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
    	UsesInitializer initializer = UseConfigurer.process(rootUse);
    	MultiConfigurerProvider configurerProvider = new MultiConfigurerProvider(initializer.providers());
    	initializer.triggers().forEach(trigger -> trigger.supplier().apply(configurerProvider).get());
    	flowConfigs.forEach((config) -> configure(new SequenceConfigurer(configurerProvider), config).get());
    }
    
    public static class TriggerSetup {

    	private final Trigger trigger;
    	
    	private Path flowName;
    	
		public TriggerSetup(Trigger trigger) {
			this.trigger = trigger;
		}
		
		public Trigger trigger() {
			return trigger;
		}
		
		public TriggerSetup flowName(Path val) {
			flowName = val;
			return this;
		}
		
		public Path flowName() {
			return flowName;
		}
    	
    }
    
    public CompletableFuture<Application> build(String identity, int applicationVersion, Application previous, final Subscriber s) {

    	CompletableFuture<Application> future = new CompletableFuture<>();
    	
    	return safelyCompletable(future, () -> {
    	
	    	UsesInitializer initializer = UseConfigurer.process(rootUse);
	    	
	    	ApplicationBuilder applicationBuilder = new ApplicationBuilder();
	    	
	    	applicationBuilder.name(applicationName);
	    	applicationBuilder.meta(meta.immutable());
	    	applicationBuilder.version(applicationVersion);
	    	applicationBuilder.initializerVisualizer(initializer.visualizer());
	    	
	    	List<Runnable> shutdownHandlers = initializer.shutdownHandlers();
	    	
	    	MultiConfigurerProvider configurerProvider = new MultiConfigurerProvider(initializer.providers());
	    	
	    	List<TriggerSetup> triggerSetups = new ArrayList<>();
	    	
	    	initializer.triggers().forEach(trigger -> {
	    		/*
	    		Path baseFlowName = applicationName.add("trigger").add(trigger.name());
	    		Path flowName = baseFlowName;
	    		int num = 1;
	    		while (flowsBuilder.roots().contains(flowName)) {
	    			flowName = baseFlowName.add(num);
	    			num++;
	    		}
	    		*/
	    		Path flowName = applicationName.add(UUID.randomUUID().toString()).add(trigger.name());
	    		TriggerSetup setup = new TriggerSetup(trigger).flowName(flowName);
	    		flowsBuilder.add(setup.flowName(), trigger.supplier().apply(configurerProvider).get());
	    		triggerSetups.add(setup);
	    	});
	    	
	    	flowConfigs.forEach((config) -> 
				flowsBuilder.add(applicationName.add(config.valueAsString()), 
						configure(new SequenceConfigurer(configurerProvider), config).get()));
	    	
	    	// ok, run the app initializer
	
	    	EverythingSubscriber subscriber = EverythingSubscriber.wrap(s);
	    	
	    	initializer.flow().run(new EverythingSubscriber() {
				
				@Override
				public void ok(MutableData data) {
					
					log.debug("initialized with [{}]", data.toPrettyJson());
					
			    	try {
			    		
						Flows flows = flowsBuilder.build(data); // constructs all the operations
						
						applicationBuilder.setFlows(flows);
						
						Registration registration = new Registration(flows);
						
						for (TriggerSetup t : triggerSetups) {
							Registration2 registration2 = new Registration2(applicationVersion, flows.flow(t.flowName()), identity);
							t.trigger().consumer().accept(registration2);
							applicationBuilder.register(registration2);
							
						}
						
						registration.resource(new SimpleDeployedResource() {
							
							@Override
							public void undeploy(int version) {
								for (Runnable handler : shutdownHandlers) {
									try {
										handler.run();
									} catch (Throwable t) {
										t.printStackTrace();
									}
								}
							}
							
						});
						
			    		applicationBuilder.register(registration);
				    	
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
			});
    	
    	});
    }

}
