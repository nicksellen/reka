package reka.core.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import reka.Identity;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.data.Data;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.setup.NetworkInfo;
import reka.core.setup.StatusProvider;

public class ApplicationBuilder {

	public final List<NetworkInfo> network = new ArrayList<>();
	
	public final List<Runnable> onUndeploy = new ArrayList<>();
	public final List<IntConsumer> pauseConsumers = new ArrayList<>();
	public final List<IntConsumer> resumeConsumers = new ArrayList<>();
	public final List<StatusProvider> statusProviders = new ArrayList<>();
	public final List<LifecycleComponent> components = new ArrayList<>();

	private Identity identity;
	private Path name;
	private Data meta;
	private int version = -1;
	private Flows flows;
	private FlowVisualizer visualizer;
	private IdentityStore store;

	public void identity(Identity identity) {
		this.identity = identity;
	}
	
	public void name(Path name) {
		this.name = name;
	}
	
	public Path name() {
		return name;
	}
	
	public void meta(Data meta) {
		this.meta = meta;
	}
	
	public void version(int version) {
		this.version = version;
	}
	
	public int version() {
		return version;
	}
	
	public void flows(Flows flows) {
		this.flows = flows;
	}
	
	public void store(IdentityStore store) {
		this.store = store;
	}
	
	public void initializerVisualizer(FlowVisualizer visualizer) {
		this.visualizer = visualizer;
	}

	public Application build() {
		return new Application(identity,
							   name, 
							   meta, 
							   version, 
							   flows,
							   store != null ? store : IdentityStore.createConcurrentIdentityStore(),
							   network, 
							   visualizer, 
							   onUndeploy,
							   pauseConsumers, 
							   resumeConsumers,
							   components,
							   statusProviders);
	}

}
