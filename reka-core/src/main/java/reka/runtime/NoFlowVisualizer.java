package reka.runtime;

import static reka.util.Path.path;
import static reka.util.Util.unsupported;
import reka.flow.builder.FlowVisualizer;
import reka.util.Path;

public class NoFlowVisualizer implements FlowVisualizer {
	
	public static final FlowVisualizer INSTANCE = new NoFlowVisualizer();
	
	private NoFlowVisualizer() {
	}

	@Override
	public Path name() {
		return path("<noflow>");
	}

	@Override
	public <T> T build(GraphVisualizer<T> graph) {
		throw unsupported();
	}

}
