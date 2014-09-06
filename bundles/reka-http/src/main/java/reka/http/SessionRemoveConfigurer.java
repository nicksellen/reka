package reka.http;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.core.builder.FlowSegments.storeSync;
import static reka.http.HttpSessionsModule.SESSION_STORE;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;
public class SessionRemoveConfigurer implements Supplier<FlowSegment> {
	
	private Function<Data,Path> keyFn = (data) -> root();
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Override
	public FlowSegment get() {
		return storeSync("session/remove", (store) -> new SessionRemoveOperation(store.get(SESSION_STORE), keyFn));
	}

}
