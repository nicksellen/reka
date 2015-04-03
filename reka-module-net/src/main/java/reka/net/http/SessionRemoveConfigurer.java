package reka.net.http;

import static reka.net.http.HttpSessionsConfigurer.SESSION_STORE;
import static reka.util.Path.dots;
import static reka.util.Path.root;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;
import reka.util.StringWithVars;

public class SessionRemoveConfigurer implements OperationConfigurer {
	
	private Function<Data,Path> keyFn = (data) -> root();
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("remove", () -> new SessionRemoveOperation(ops.ctx().get(SESSION_STORE), keyFn));
	}

}
