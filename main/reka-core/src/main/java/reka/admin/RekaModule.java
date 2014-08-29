package reka.admin;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
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
import reka.config.processor.DocConverter;
import reka.config.processor.IncludeConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;

import com.google.common.base.Charsets;

public class RekaModule extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(RekaModule.class);
	
	private final ApplicationManager manager;
	
	private final List<ConfigBody> deployHandlers = new ArrayList<>();
	private final List<ConfigBody> undeployHandlers = new ArrayList<>();
	
	public RekaModule(ApplicationManager manager) {
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
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
	@Override
	public void setup(ModuleSetup module) {
		module.operation(path("list"), () -> new RekaListConfigurer(manager));
		module.operation(path("get"), () -> new RekaDetailsConfigurer(manager));
		module.operation(path("validate"), (provider) -> new RekaValidateConfigurer(provider, manager));
		module.operation(path("deploy"), () -> new RekaDeployConfigurer(manager));
		module.operation(path("undeploy"), () -> new RekaUndeployConfigurer(manager));
		module.operation(path("redeploy"), () -> new RekaRedeployConfigurer(manager));
		module.operation(path("visualize"), () -> new RekaVisualizeConfigurer(manager));
		
		for (ConfigBody body : deployHandlers) {			
			module.trigger("deploy", body, registration -> {
				Flow flow = registration.flow();
				manager.addDeployListener(flow);
				registration.undeploy(version -> { 
					manager.removeDeployListener(flow);
				});
			});
		}		

		for (ConfigBody body : undeployHandlers) {			
			module.trigger("undeploy", body, registration -> {
				Flow flow = registration.flow();
				manager.addUndeployListener(flow);
				registration.undeploy(version -> { 
					manager.removeUndeployListener(flow);
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
			return new String(content.asBytes(), Charsets.UTF_8);
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
				new DocConverter()));
		
		return processor.process(config);
		
	}
	
}
