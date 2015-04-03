package reka.data;

import java.util.function.Supplier;

import reka.api.Path;
import reka.data.content.Content;

public class BasicContentReader implements Supplier<Content> {

	private final Data store;
	private final Path path;
	
	public BasicContentReader(Data store, Path path) {
		this.store = store;
		this.path = path;
	}


	@Override
	public Content get() {
		return store.getContent(path).get();
	}
	
}
