package reka.core.bundle;

import java.util.ArrayList;
import java.util.List;

import reka.DeployedResource;
import reka.api.data.Data;
import reka.core.builder.Flows;

public class Registration {
	
	private final Flows flows;
	private final List<DeployedResource> resources = new ArrayList<>();
	private final List<PortAndProtocol> ports = new ArrayList<>();
	
	public Registration(Flows flows) {
		this.flows = flows;
	}

	public Flows flows() {
		return flows;
	}
	
	public void resource(DeployedResource resource) {
		resources.add(resource);
	}

	public void protocol(int port, String protocol, Data data) {
		ports.add(new PortAndProtocol(port, protocol, data));
	}
	
	public List<DeployedResource> resources() {
		return resources;
	}
	
	public List<PortAndProtocol> ports() {
		return ports;
	}
	
}