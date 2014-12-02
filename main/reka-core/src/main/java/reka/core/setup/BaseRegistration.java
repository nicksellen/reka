package reka.core.setup;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import reka.api.IdentityStore;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

abstract class BaseRegistration {

	final int applicationVersion;
	final String identity;
	final IdentityStore store;

	final List<NetworkInfo> network;
	
	final List<IntConsumer> undeployConsumers;
	final List<IntConsumer> pauseConsumers;
	final List<IntConsumer> resumeConsumers;
	
	public BaseRegistration(
			int applicationVersion, 
			String identity,
			IdentityStore store,
			List<NetworkInfo> network,
			List<IntConsumer> undeployConsumers,
			List<IntConsumer> pauseConsumers,
			List<IntConsumer> resumeConsumers) {
		this.applicationVersion = applicationVersion;
		this.identity = identity;
		this.store = store;
		this.network = network;
		this.undeployConsumers = undeployConsumers;
		this.pauseConsumers = pauseConsumers;
		this.resumeConsumers = resumeConsumers;
	}

	public int applicationVersion() {
		return applicationVersion;
	}
	
	public String applicationIdentity() {
		return identity;
	}
	
	public IdentityStore store() {
		return store;
	}
	
	public void onUndeploy(IntConsumer c) {
		undeployConsumers.add(c);
	}
	
	public void onPause(IntConsumer c) {
		pauseConsumers.add(c);
	}
	
	public void onResume(IntConsumer c) {
		resumeConsumers.add	(c);
	}
	
	public List<IntConsumer> undeployConsumers() {
		return undeployConsumers;
	}
	
	public List<IntConsumer> pauseConsumers() {
		return pauseConsumers;
	}

	public List<IntConsumer> resumeConsumers() {
		return resumeConsumers;
	}
	
	public void network(int port, String protocol) {
		network(port, protocol, data -> {});
	}
	
	public void network(int port, String protocol, Consumer<MutableData> details) {
		MutableData data = MutableMemoryData.create();
		details.accept(data);
		network.add(new NetworkInfo(port, protocol, data.immutable()));
	}
	
	public List<NetworkInfo> network() {
		return network;
	}
	
}