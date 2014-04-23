package reka;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.bundle.DefaultTriggerSetup.OnStart;
import reka.core.bundle.PortAndProtocol;

public class ApplicationBuilder {

	private final List<IntConsumer> undeploys = new ArrayList<>();
	private final List<PortAndProtocol> ports = new ArrayList<>();

	private Path name;
	private int version = -1;
	private Flows flows;
	private FlowVisualizer visualizer;

	public void setName(Path name) {
		this.name = name;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}
	
	public void setFlows(Flows flows) {
		this.flows = flows;
	}
	
	public void setInitializerVisualizer(FlowVisualizer visualizer) {
		this.visualizer = visualizer;
	}

	public void registerThings(OnStart s) {
		undeploys.addAll(s.undeploys());
		ports.addAll(s.ports());
	}

	public Application build() {
		return new Application(name, version, flows, undeploys, ports, visualizer);
	}

}
