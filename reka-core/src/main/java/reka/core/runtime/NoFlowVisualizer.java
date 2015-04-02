package reka.core.runtime;

import static reka.api.Path.path;
import static reka.util.Util.unsupported;
import reka.api.Path;
import reka.core.builder.FlowVisualizer;

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
