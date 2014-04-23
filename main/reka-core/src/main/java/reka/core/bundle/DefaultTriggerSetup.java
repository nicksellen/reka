package reka.core.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.data.Data;
import reka.core.builder.Flows;

public class DefaultTriggerSetup implements SetupTrigger {
	
	private final String identity;
	private final Path applicationName;
	private final Set<Path> requiresFlows = new HashSet<>();
	private final List<Consumer<Contructed>> onStarts = new ArrayList<>();
	
	public DefaultTriggerSetup(String identity, Path applicationName) {
		this.identity = identity;
		this.applicationName = applicationName;
	}
	
	@Override
	public Path applicationName() {
		return applicationName;
	}
	
	@Override
	public String identity() {
		return identity;
	}

	@Override
	public void requiresFlow(Path flowName) {
		// FIXME: namespacing?
		requiresFlows.add(flowName);
	}

	@Override
	public void requiresFlows(Collection<Path> flowNames) {
		// FIXME: namespacing?
		requiresFlows.addAll(flowNames);
	}

	@Override
	public void onStart(Consumer<Contructed> onStart) {
		onStarts.add(onStart);
	}
	
	public List<Consumer<Contructed>> onStarts() {
		return onStarts;
	}
	
	public static class OnStart implements Contructed {
		
		private final Flows flows;
		private final List<IntConsumer> undeploys = new ArrayList<>();
		private final List<PortAndProtocol> ports = new ArrayList<>();
		
		public OnStart(Flows flows) {
			this.flows = flows;
		}

		@Override
		public Flows flows() {
			return flows;
		}

		@Override
		public void onUndeploy(IntConsumer run) {
			undeploys.add(run);
		}

		@Override
		public void register(int port, String protocol, Data data) {
			ports.add(new PortAndProtocol(port, protocol, data));
		}

		public List<IntConsumer> undeploys() {
			return undeploys;
		}
		
		public List<PortAndProtocol> ports() {
			return ports;
		}
		
	}

}
