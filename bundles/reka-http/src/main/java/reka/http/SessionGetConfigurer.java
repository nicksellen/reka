package reka.http;

import static reka.api.Path.dots;
import static reka.http.HttpSessionsModule.SESSION_STORE;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

public class SessionGetConfigurer implements OperationConfigurer {
	
	private Function<Data,Path> keyFn;
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("session/get", store -> new SessionGetOperation(store.get(SESSION_STORE), keyFn));
	}

}
