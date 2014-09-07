package reka.http;

import static reka.api.Path.dots;
import static reka.http.HttpSessionsModule.SESSION_STORE;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class SessionPutConfigurer implements OperationsConfigurer {
	
	private Function<Data,Path> keyFn;
	private Function<Data,String> valFn;
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}
	
	@Conf.Val
	public void val(String val) {
		valFn = StringWithVars.compile(val);
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("session/put", store -> new SessionPutOperation(store.get(SESSION_STORE), keyFn, valFn));
	}

}
