package reka.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.util.Path;

@FunctionalInterface
public interface DiffPathConsumer {
	
	static final Logger log = LoggerFactory.getLogger(DiffPathConsumer.class);
	
	public static enum DiffPathType {
		ADDED, REMOVED, CHANGED
	}
	
	void accept(Path path, DiffPathConsumer.DiffPathType type);
	
	public static class DiffPathPrinter implements DiffPathConsumer {

		@Override
		public void accept(Path path, DiffPathType type) {
			switch (type) {
			case ADDED:
				log.debug("+ {}", path.dots());
				break;
			case REMOVED:
				log.debug("- {}", path.dots());
				break;
			case CHANGED:
				log.debug("* {}", path.dots());
				break;
			}
		}
	}
}