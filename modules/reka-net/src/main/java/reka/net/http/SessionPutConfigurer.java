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

public class SessionPutConfigurer implements OperationConfigurer {
	
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
		ops.add("put", store -> new SessionPutOperation(store.get(SESSION_STORE), keyFn, valFn));
	}

}
