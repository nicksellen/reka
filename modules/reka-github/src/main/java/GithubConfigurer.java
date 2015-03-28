import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationConfigurer;
import reka.net.NetManager;

public class GithubConfigurer extends ModuleConfigurer {
	
	private final NetManager server;

	private String secret;
	private final List<Function<ConfigurerProvider,OperationConfigurer>> requestHandlers = new ArrayList<>();
	
	public GithubConfigurer(NetManager server) {
		this.server = server;
	}
	
	@Conf.At("secret")
	public void secret(String val) {
		secret = val;
	}
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "webhook":
			requestHandlers.add(provider -> configure(new SequenceConfigurer(provider), config.body()));
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	@Override
	public void setup(ModuleSetup module) {
		for (Function<ConfigurerProvider, OperationConfigurer> h : requestHandlers) {
			module.buildFlow("on webhook", h, reg -> {
				
				
			});
		}
	}

}
