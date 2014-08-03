package reka.core.builder;

import static java.lang.String.format;

import java.util.List;

import reka.api.Path;
import reka.api.data.Data;
import reka.core.builder.FlowVisualizer.GraphVisualizer;
import reka.core.builder.FlowVisualizer.NodeType;

public class DagreD3GraphVisualizer implements GraphVisualizer<String> {
	
	private final StringBuilder sb = new StringBuilder();
	
	public DagreD3GraphVisualizer() {
		sb.append("var g = new dagreD3.Digraph();").append('\n');
	}

	@Override
	public void node(int id, String name, NodeType type) {
		sb.append(format("g.addNode('%s', { label: '%s' });", id, name)).append('\n');
	}

	@Override
	public void group(Path path, int id) {
	}

	@Override
	public void connect(int from, int to, String label, boolean optional) {
		sb.append("g.addEdge(");
		sb.append(format("null, '%s', '%s', { label: '%s' });", from, to, label != null ? label : ""));
		
		/*
			sb.append(format("null, %s, %s", from, to));
			sb.append(", ");
			if (label != null && !"".equals(label)) {
				sb.append(format("{ label: '%s' }", label));
			} else {
				sb.append("{}");
			}
		sb.append(");");
			*/
		sb.append('\n');
	}

	@Override
	public String build() {
		return sb.toString();
	}

	@Override
	public void meta(int id, List<Data> metas) {
		
	}
	
}