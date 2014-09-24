package reka.core.setup;

import java.util.List;
import java.util.function.IntConsumer;

import reka.api.IdentityStore;
import reka.api.data.Data;

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
	
	public void undeploy(IntConsumer c) {
		undeployConsumers.add(c);
	}
	
	public void pause(IntConsumer c) {
		pauseConsumers.add(c);
	}
	
	public void resume(IntConsumer c) {
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
	
	public void network(int port, String protocol, Data details) {
		network.add(new NetworkInfo(port, protocol, details));
	}
	
	public List<NetworkInfo> network() {
		return network;
	}
	
}