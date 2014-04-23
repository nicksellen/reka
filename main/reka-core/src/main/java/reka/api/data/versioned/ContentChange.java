package reka.api.data.versioned;

import static java.lang.String.format;
import reka.api.Path;
import reka.api.content.Content;

public interface ContentChange {

	public Path path();
	
	public enum ChangeType {
		ADDED, REMOVED, MODIFIED;
		public String symbol() {
			switch(this) {
			case ADDED: return "+";
			case REMOVED: return "-";
			case MODIFIED: return "*";
			default:
				throw new RuntimeException("ahh");
			}
		}
	}
	
	public ChangeType changeType();
	public Content content();

	public static final class Added implements ContentChange {
		
		private final Path path;
		private final Content content;
		
		public Added(Path path, Content content) {
			this.path = path;
			this.content = content;
		}
	
		@Override
		public ChangeType changeType() {
			return ChangeType.ADDED;
		}
		
		public Content content() {
			return content;
		}
		
		public Path path() {
			return path;
		}

		@Override
		public String toString() {
			return format("ADDED %s [%s]", path.slashes(), content);
		}
		
	}

	public static final class Removed implements ContentChange {
		
		private final Path path;
		private final Content content;
		
		public Removed(Path path, Content content) {
			this.path = path;
			this.content = content;
		}
	
		@Override
		public ChangeType changeType() {
			return ChangeType.REMOVED;
		}
		
		public Path path() {
			return path;
		}
		
		public Content content() {
			return content;
		}

		@Override
		public String toString() {
			return format("REMOVED %s [%s]", path.slashes(), content);
		}
		
	}

	public static final class Modified implements ContentChange {
		
		private final Path path;
		private final Content from;
		private final Content to;
		
		public Modified(Path path, Content from, Content to) {
			this.path = path;
			this.from = from;
			this.to = to;
		}
	
		@Override
		public ChangeType changeType() {
			return ChangeType.MODIFIED;
		}
		
		public Path path() {
			return path;
		}
		
		public Content from() {
			return from;
		}
		
		public Content to() {
			return to;
		}
		
		public Content content() {
			return to;
		}

		@Override
		public String toString() {
			return format("MODIFIED %s [%s] -> [%s]", path.slashes(), from, to);
		}
		
	}
}