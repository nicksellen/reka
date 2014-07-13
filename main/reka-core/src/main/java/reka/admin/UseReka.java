package reka.admin;

import static reka.util.Util.runtime;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.config.processor.CommentConverter;
import reka.config.processor.DocConverter;
import reka.config.processor.IncludeConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

import com.google.common.base.Charsets;

public class UseReka extends UseConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(UseReka.class);
	
	private final ApplicationManager manager;
	
	public UseReka(ApplicationManager manager) {
		this.manager = manager;
	}

	@Override
	public void setup(UseInit use) {
		use.operation("list", () -> new RekaListConfigurer(manager));
		use.operation("get", () -> new RekaDetailsConfigurer(manager));
		use.operation("validate", (provider) -> new RekaValidateConfigurer(provider, manager));
		use.operation("deploy", () -> new RekaDeployConfigurer(manager));
		use.operation("undeploy", () -> new RekaUndeployConfigurer(manager));
		use.operation("redeploy", () -> new RekaRedeployConfigurer(manager));
		use.operation("visualize", () -> new RekaVisualizeConfigurer(manager));
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
