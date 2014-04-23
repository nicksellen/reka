package reka.core.builder;

import reka.api.Path;

public interface FlowVisualizer {
	
	Path name();
	<T> T build(GraphVisualizer<T> graph);

	public static enum NodeType {
		NORMAL, START, END, EMBEDDED
	}
	
	public static interface GraphVisualizer <T> {
		public void node(int id, String name, NodeType type);
		public void group(Path path, int id);
		public void connect(int from, int to, String label, boolean optional);
		public T build();
	}
	
}