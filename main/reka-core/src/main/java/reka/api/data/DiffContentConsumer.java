package reka.api.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;

@FunctionalInterface
public interface DiffContentConsumer {
	
	static final Logger log = LoggerFactory.getLogger(DiffContentConsumer.class);
	
	public static enum DiffContentType {
		ADDED, REMOVED, CHANGED
	}
	
	void accept(Path path, DiffContentConsumer.DiffContentType type, Content prev, Content current);
	
	public static class DiffContentPrinter implements DiffContentConsumer {

		@Override
		public void accept(Path path, DiffContentType type, Content prev,
				Content current) {
			switch (type) {
			case ADDED:
				log.debug("+ {} {}", path.dots(), current);
				break;
			case REMOVED:
				log.debug("- {} {}", path.dots(), prev);
				break;
			case CHANGED:
				log.debug("* {} {} -> {}", path.dots(), prev, current);
				break;
			}
		}
	}
	
}