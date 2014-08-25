package reka.http;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class SessionRemoveConfigurer implements Supplier<FlowSegment> {
	
	private final Path storePath;
	
	private Function<Data,Path> keyFn = (data) -> root();
	
	public SessionRemoveConfigurer(Path storePath) {
		this.storePath = storePath;
	}
	
	@Conf.Subkey
	public void key(String val) {
		keyFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Override
	public FlowSegment get() {
		return sync("session/remove", (data) -> new SessionRemoveOperation(data.at(storePath).content().valueAs(SessionStore.class), keyFn));
	}

}
