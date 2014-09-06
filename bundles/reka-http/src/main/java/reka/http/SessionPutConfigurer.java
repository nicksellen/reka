package reka.http;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.storeSync;
import static reka.http.HttpSessionsModule.SESSION_STORE;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class SessionPutConfigurer implements Supplier<FlowSegment> {
	
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
	public FlowSegment get() {
		return storeSync("session/put", (store) -> new SessionPutOperation(store.get(SESSION_STORE), keyFn, valFn));
	}

}
