package reka.core.builder;

import java.util.List;

import reka.api.Path;
import reka.api.data.Data;

public interface FlowVisualizer {
	
	Path name();
	<T> T build(GraphVisualizer<T> graph);

	public static enum NodeType {
		NORMAL, START, END, REFERENCE
	}
	
	public static interface GraphVisualizer <T> {
		void node(int id, String name, NodeType type);
		void group(Path path, int id);
		void connect(int from, int to, String label, boolean optional);
		T build();
		void meta(int id, List<Data> metas);
	}
	
}