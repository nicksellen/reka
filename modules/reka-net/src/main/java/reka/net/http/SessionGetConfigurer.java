package reka.net.http;

import static reka.api.Path.dots;
import static reka.net.http.HttpSessionsConfigurer.SESSION_STORE;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class SessionGetConfigurer implements OperationConfigurer {
	
	private Function<Data,Path> keyFn;
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("get", store -> new SessionGetOperation(store.get(SESSION_STORE), keyFn));
	}

}