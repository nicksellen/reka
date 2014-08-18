package reka.http;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class SessionPutConfigurer implements Supplier<FlowSegment> {
	
	private final Path storePath;
	
	private Function<Data,Path> keyFn;
	private Function<Data,String> valFn;
	
	public SessionPutConfigurer(Path storePath) {
		this.storePath = storePath;
	}
	
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
		return sync("session/put", (data) -> new SessionPutOperation(data.at(storePath).content().valueAs(SessionStore.class), keyFn, valFn));
	}

}
