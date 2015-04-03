package reka.net.http;

import static reka.api.Path.dots;
import static reka.net.http.HttpSessionsConfigurer.SESSION_STORE;

import java.util.function.Function;

import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class SessionGetConfigurer implements OperationConfigurer {
	
	private Function<Data,Path> keyFn;
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("get", () -> new SessionGetOperation(ops.ctx().get(SESSION_STORE), keyFn));
	}

}
