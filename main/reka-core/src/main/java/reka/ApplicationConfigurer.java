package reka;

import static java.util.stream.Collectors.toList;
import static reka.api.Path.path;
import static reka.api.Path.slashes;
import static reka.configurer.Configurer.configure;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.configurer.Configurer.Preconditions.invalidConfig;
import static reka.util.Util.createEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
import reka.configurer.Configurer.ErrorCollector;
import reka.configurer.ErrorReporter;
import reka.configurer.annotations.Conf;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.builder.FlowsBuilder;
import reka.core.bundle.BundleManager;
import reka.core.bundle.DefaultTriggerSetup;
import reka.core.bundle.SetupTrigger;
import reka.core.bundle.SetupTrigger.Contructed;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseConfigurer.UsesInitializer;
import reka.core.config.MultiConfigurerProvider;
import reka.core.config.SequenceConfigurer;

public class ApplicationConfigurer implements ErrorReporter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
    private Path applicationName;
    private Number version;
    
    @SuppressWarnings("unused") // one day!
	private String description;
    
    public ApplicationConfigurer(BundleManager bundles) {
        rootUse = new UseRoot();
		rootUse.mappings(bundles.uses());
    }

    private final FlowsBuilder flows = new FlowsBuilder();
    
    private final List<Config> flowConfigs = new ArrayList<>();
    
    private final UseConfigurer rootUse;
    
    private final List<Entry<Path,Config>> triggerConfigs = new ArrayList<>();
    
    @Conf.At("name")
    public void name(String val) {
        applicationName = slashes(val);
    }
    
    @Conf.At("version")
    public void version(BigDecimal val) {
    	version = val;
    	log.debug("version: {}", version);
    }
    
    @Conf.At("description")
    public void description(Config config) {
    	if (config.hasDocument()) {
    		description = config.documentContentAsString();
    	} else if (config.hasValue()) {
    		description = config.valueAsString();
    	} else {
    		invalidConfig("must be a document or a value");
    	}
    }
    
    @Conf.Each("use")
    public void use(Config config) {
    	checkConfig(config.hasBody(), "must have a body");
		rootUse.use(config);
    }

    @Conf.Each("trigger")
    @Conf.Each("export")
    public void trigger(Config config) {
    	if (config.hasBody()) {
    		for (Config child : config.body()) {
    			trigger(slashes(child.key()), child);	
    		}
    	} else {
    		invalidConfig("must have value or body");
    	}
    }
    
    private void trigger(Path key, Config config) {
    	triggerConfigs.add(createEntry(key, config));
    }
    
    @Conf.Each("run")
    public void run(Config config) {
    	checkConfig(config.hasValue(), "you must provide a value/name");
    	flowConfigs.add(config);
    }

    @Conf.Each("flow")
    public void flow(Config config) {
    	run(config);
    }

	@Override
	public void errors(ErrorCollector errors) {
		if (applicationName == null) errors.add("name is required");
		if (triggerConfigs.isEmpty()) errors.add("please add at least one trigger");
		if (flowConfigs.isEmpty()) errors.add("please add at least one flow (with 'run', or 'flow')");
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
		
    	configuredFlows.forEach((name, segment) -> flows.add(name, segment.get()));
    	
    	return flows.buildVisualizers();
    }

    public CompletableFuture<Application> build(String identity, int version) {
    	return build(identity, version, null);
    }
    
    public CompletableFuture<Application> build(String identity, int version, Application previous) {
    	return build(identity, version, previous, EverythingSubscriber.DO_NOTHING);
    }
    
    private void configureTriggers(Map<Path, Supplier<TriggerConfigurer>> triggerConfigurers, SetupTrigger triggerSetup) {
    	
		triggerConfigs.forEach(e -> {
			
			Path path = e.getKey();
			Config config = e.getValue();
			
			checkConfig(triggerConfigurers.containsKey(path), "we don't have a trigger configurer for [%s] try one of %s", 
					path.slashes(), 
					triggerConfigurers.keySet().stream().map(Path::slashes).collect(toList()));
			
			configure(triggerConfigurers.get(path).get(), config).setupTriggers(triggerSetup);
			
		});
		
    }
    
    public CompletableFuture<Application> build(String identity, int version, Application previous, final Subscriber s) {

    	CompletableFuture<Application> future = new CompletableFuture<>();
    	
    	UsesInitializer initializer = UseConfigurer.process(rootUse);
    	
    	ApplicationBuilder application = new ApplicationBuilder();
    	
    	application.setName(applicationName);
    	application.setVersion(version);
    	application.setInitializerVisualizer(initializer.visualizer());
    	
    	Map<Path, Supplier<TriggerConfigurer>> triggerConfigurers = initializer.triggers();

    	MultiConfigurerProvider provider = new MultiConfigurerProvider(initializer.providers());
    	
    	flowConfigs.forEach((config) -> 
			flows.add(applicationName.add(config.valueAsString()), 
					configure(new SequenceConfigurer(provider), config).get()));
    	
    	DefaultTriggerSetup triggerSetup = new DefaultTriggerSetup(identity, applicationName);

    	configureTriggers(triggerConfigurers, triggerSetup);
    	
    	// ok, run the app initializer

    	EverythingSubscriber subscriber = EverythingSubscriber.wrap(s);
    	
    	initializer.flow().run(new EverythingSubscriber() {
			
			@Override
			public void ok(MutableData data) {
				
				log.debug("initialized with [{}]", data.toPrettyJson());
				
		    	try {
		    		
					Flows f = flows.build(data); // constructs all the operations
					
					application.setFlows(f);
					
					DefaultTriggerSetup.OnStart s = new DefaultTriggerSetup.OnStart(f);
					
					for (Consumer<Contructed> onStart : triggerSetup.onStarts()) {
						onStart.accept(s);
					}
					
		    		application.registerThings(s);
			    	
			    	future.complete(application.build());
			    	
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
    	
    	return future;
    }

}
