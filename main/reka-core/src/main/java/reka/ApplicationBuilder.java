package reka;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.data.Data;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.setup.NetworkInfo;

public class ApplicationBuilder {

	private final List<NetworkInfo> network = new ArrayList<>();
	
	private final List<IntConsumer> undeployConsumers = new ArrayList<>();
	private final List<IntConsumer> pauseConsumers = new ArrayList<>();
	private final List<IntConsumer> resumeConsumers = new ArrayList<>();

	private Path name;
	private Data meta;
	private int version = -1;
	private Flows flows;
	private FlowVisualizer visualizer;

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
	
	public void initializerVisualizer(FlowVisualizer visualizer) {
		this.visualizer = visualizer;
	}

	public List<NetworkInfo> network() {
		return network;
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

	public Application build() {
		return new Application(name, meta, version, flows, network, visualizer, undeployConsumers, pauseConsumers, resumeConsumers);
	}

}
