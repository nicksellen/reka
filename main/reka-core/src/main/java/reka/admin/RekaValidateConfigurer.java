package reka.admin;

import static reka.api.Path.dots;
import static reka.config.configurer.Configurer.configure;
import static reka.util.Util.runtime;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

public class RekaValidateConfigurer implements OperationConfigurer {

	private final ConfigurerProvider provider;
	private final ApplicationManager manager;
	
	private Path in;
	private Function<Data,String> filenameFn;
	
	private OperationConfigurer whenOk;
	private OperationConfigurer whenError;
	
	RekaValidateConfigurer(ConfigurerProvider provider, ApplicationManager manager) {
		this.provider = provider;
		this.manager = manager;
	}
	
	@Conf.At("in")
	public void in(String val) {
		in = dots(val);
	}
	
	@Conf.At("filename")
	public void filename(String val) {
		filenameFn = StringWithVars.compile(val);
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
		
		if (in != null) {
			ops.addRouter("validate", store -> new RekaValidateFromContentOperation(manager, in));
		} else if (filenameFn != null) {
			ops.addRouter("validate", store -> new RekaValidateFromFileOperation(manager, filenameFn));
		} else {
			throw runtime("must specify either 'in' or 'filename'");
		}
		ops.parallel(par -> {
			if (whenOk != null) par.route("ok", whenOk);
			if (whenError != null) par.route("error", whenError);
		});
	}
	
}