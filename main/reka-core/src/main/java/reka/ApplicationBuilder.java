package reka;

import java.util.ArrayList;
import java.util.List;

import reka.api.Path;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.bundle.PortAndProtocol;
import reka.core.bundle.Registration;

public class ApplicationBuilder {

	private final List<PortAndProtocol> ports = new ArrayList<>();
	private final List<DeployedResource> resources = new ArrayList<>();

	private Path name;
	private int version = -1;
	private Flows flows;
	private FlowVisualizer visualizer;

	public void name(Path name) {
		this.name = name;
	}
	
	public void version(int version) {
		this.version = version;
	}
	
	public void setFlows(Flows flows) {
		this.flows = flows;
	}
	
	public void initializerVisualizer(FlowVisualizer visualizer) {
		this.visualizer = visualizer;
	}

	public void register(Registration registration) {
		resources.addAll(registration.resources());
		ports.addAll(registration.ports());
	}

	public Application build() {
		return new Application(name, version, flows, ports, visualizer, resources);
	}

}
