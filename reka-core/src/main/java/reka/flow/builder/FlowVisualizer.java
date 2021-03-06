package reka.flow.builder;

import java.util.List;

import reka.data.Data;
import reka.util.Path;

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