package reka;

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.setup.NetworkInfo;

public class Application {
 

	private final Path name;
	private final Data meta;
	private final String fullName;
	private final int version;
	private final Flows flows;
	private final FlowVisualizer initializerVisualizer;


	private final List<NetworkInfo> network = new ArrayList<>();
	
	private final List<IntConsumer> undeployConsumers = new ArrayList<>();
	private final List<IntConsumer> pauseConsumers = new ArrayList<>();
	private final List<IntConsumer> resumeConsumers = new ArrayList<>();
	
	public Application(
			Path name, 
			Data meta,
			int version, 
			Flows flows,  
			List<NetworkInfo> network, 
			FlowVisualizer initializerVisualizer,
			List<IntConsumer> undeployConsumers,
			List<IntConsumer> pauseConsumers,
			List<IntConsumer> resumeConsumers) {
		this.name = name;
		this.fullName = name.slashes();
		this.meta = meta;
		this.version = version;
		this.flows = flows;
		this.initializerVisualizer = initializerVisualizer;
		this.network.addAll(network);
		this.undeployConsumers.addAll(undeployConsumers);
		this.pauseConsumers.addAll(pauseConsumers);
		this.resumeConsumers.addAll(resumeConsumers);
		this.network.sort(naturalOrder());
	}
		
	public Path name() {
		return name;
	}
	
	public Data meta() {
		return meta;
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
	
	public List<NetworkInfo> network() {
		return network;
	}
	
	public FlowVisualizer initializerVisualizer() {
		return initializerVisualizer;
	}
	
	public Collection<Path> flowNames() {
		List<Path> names = new ArrayList<>();
		for (Flow flow : flows.all()) {
			names.add(flow.name());
		}
		names.sort(naturalOrder());
		return names;
	}
	
	public void undeploy() {
		undeployConsumers.forEach(c -> { 
			try {
				c.accept(version);
			} catch (Throwable t) {
				t.printStackTrace();
			}	
		});
	}
	
	public void pause() {
		pauseConsumers.forEach(c -> { 
			try {
				c.accept(version);
			} catch (Throwable t) {
				t.printStackTrace();
			}	
		});
	}
	
	public void resume() {
		resumeConsumers.forEach(c -> { 
			try {
				c.accept(version);
			} catch (Throwable t) {
				t.printStackTrace();
			}	
		});
	}

}
