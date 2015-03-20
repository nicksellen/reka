package reka.admin;

import static reka.config.configurer.Configurer.configure;
import static reka.util.Util.runtime;

import java.util.UUID;
import java.util.function.Function;

import reka.api.data.Data;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.app.manager.ApplicationManager;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class RekaValidateConfigurer implements OperationConfigurer {

	private final ConfigurerProvider provider;
	private final ApplicationManager manager;
	
	private Function<Data,String> filenameFn;
	private Function<Data,String> identityFn = (unused) -> UUID.randomUUID().toString();
	
	private OperationConfigurer whenOk;
	private OperationConfigurer whenError;
	
	RekaValidateConfigurer(ConfigurerProvider provider, ApplicationManager manager) {
		this.provider = provider;
		this.manager = manager;
	}
	
	@Conf.At("filename")
	public void filename(String val) {
		filenameFn = StringWithVars.compile(val);
	}

	@Conf.At("identity")
	public void identity(String val) {
		identityFn = StringWithVars.compile(val);
	}

	@Conf.Each("when")
	public void when(Config config) {
		switch (config.valueAsString()) {
		case "ok":
			whenOk = ops(config.body());
			break;
		case "error":
			whenError = ops(config.body());
			break;
		default:
			throw runtime("no when case for %s", config.valueAsString());
		}
	}

	
	private OperationConfigurer ops(ConfigBody body) {
		return configure(new SequenceConfigurer(provider), body);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.router("validate", ctx -> new RekaValidateFromFileOperation(manager, filenameFn, identityFn), routes -> {
			if (whenOk != null) routes.add(RekaValidateFromFileOperation.OK, whenOk);
			if (whenError != null) routes.add(RekaValidateFromFileOperation.ERROR, whenError);
		});
	}
	
}