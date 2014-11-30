package reka.admin;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
import reka.ApplicationManager.EventType;
import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.NavigableConfig;
import reka.config.configurer.annotations.Conf;
import reka.config.parser.ConfigParser;
import reka.config.processor.CommentConverter;
import reka.config.processor.DocumentationConverter;
import reka.config.processor.IncludeConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class AdminConfigurer extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(AdminConfigurer.class);
	
	private final ApplicationManager manager;
	
	private final List<ConfigBody> deployHandlers = new ArrayList<>();
	private final List<ConfigBody> undeployHandlers = new ArrayList<>();
	private final List<ConfigBody> statusHandlers = new ArrayList<>();
	
	public AdminConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}

	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "deploy":
			deployHandlers.add(config.body());
			break;
		case "undeploy":
			undeployHandlers.add(config.body());
			break;
		case "status":
			statusHandlers.add(config.body());
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
	@Override
	public void setup(ModuleSetup module) {
		module.operation(path("list"), provider -> new RekaListConfigurer(manager));
		module.operation(path("get"), provider -> new RekaDetailsConfigurer(manager));
		module.operation(path("validate"), provider -> new RekaValidateConfigurer(provider, manager));
		module.operation(path("unpack"), provider -> new RekaUnpackConfigurer(dirs()));
		module.operation(path("deploy"), provider -> new RekaDeployConfigurer(manager, dirs()));
		module.operation(path("undeploy"), provider -> new RekaUndeployConfigurer(manager, dirs()));
		module.operation(path("visualize"), provider -> new RekaVisualizeConfigurer(manager));
		
		for (ConfigBody body : deployHandlers) {			
			module.trigger("on deploy", body, registration -> {
				Flow flow = registration.flow();
				manager.addListener(flow, EventType.deploy);
				registration.undeploy(version -> { 
					manager.removeListener(flow);
				});
			});
		}		

		for (ConfigBody body : undeployHandlers) {			
			module.trigger("on undeploy", body, registration -> {
				Flow flow = registration.flow();
				manager.addListener(flow, EventType.undeploy);
				registration.undeploy(version -> { 
					manager.removeListener(flow);
				});
			});
		}	

		for (ConfigBody body : statusHandlers) {			
			module.trigger("on status", body, registration -> {
				Flow flow = registration.flow();
				manager.addListener(flow, EventType.status);
				registration.undeploy(version -> { 
					manager.removeListener(flow);
				});
			});
		}
		
	}
	
	static String getConfigStringFromData(Data data, Path in) {
		
		Optional<Content> o = data.at(in).firstContent();
		
		if (!o.isPresent()) {
			throw runtime("couldn'd find config at [%s] from [%s]", in, data.toPrettyJson());
		}
		
		Content content = o.get();
		
		if (content.hasByteBuffer()) {
			return new String(content.asBytes(), StandardCharsets.UTF_8);
		} else if (content.hasValue()) {
			return content.asUTF8();
		} else {
			throw runtime("can't find configuration!");
		}
		
	}

	// TODO: remove this method
	static NavigableConfig getConfigFromData(Data data, Path in) {
		
		log.warn("this needs to be removed, it doesn't process the config in the same way as it originally would have!!!");
				
		NavigableConfig config = ConfigParser.fromString(getConfigStringFromData(data, in));
		
		Processor processor = new Processor(new MultiConverter(
				new CommentConverter(),
				new IncludeConverter(), 
				new DocumentationConverter()));
		
		return processor.process(config);
		
	}
	
}
