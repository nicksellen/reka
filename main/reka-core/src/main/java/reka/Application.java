package reka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.flow.Flow;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.bundle.PortAndProtocol;

public class Application {

	private final List<IntConsumer> undeploys;
	private final List<PortAndProtocol> ports; 

	private final Path name;
	private final String fullName;
	private final int version;
	private final Flows flows;
	private final FlowVisualizer initializerVisualizer;
	
	public Application(Path name, int version, Flows flows, List<IntConsumer> undeploys, List<PortAndProtocol> ports, FlowVisualizer initializerVisualizer) {
		this.name = name;
		this.fullName = name.slashes();
		this.version = version;
		this.flows = flows;
		this.undeploys = undeploys;
		this.ports = ports;
		this.initializerVisualizer = initializerVisualizer;
	}
		
	public Path name() {
		return name;
	}
	
	public String fullName() {
		return fullName;
	}
	
	public int version() {
		return version;
	}
	
	public Flows flows() {
		return flows;
	}
	
	public List<PortAndProtocol> ports() {
		return ports;
	}
	
	public FlowVisualizer initializerVisualizer() {
		return initializerVisualizer;
	}
	
	public Collection<Path> flowNames() {
		List<Path> names = new ArrayList<>();
		for (Flow flow : flows.flows()) {
			names.add(flow.name());
		}
		return names;
	}
	
	public void undeploy() {
		for (IntConsumer undeploy : undeploys) {
			try {
				undeploy.accept(version);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

}
