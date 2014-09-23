package reka;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

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
import reka.core.setup.StatusProvider;
import reka.core.setup.StatusProvider.StatusReport;

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
	private final List<StatusProvider> statusProviders = new ArrayList<>();
	
	public Application(
			Path name, 
			Data meta,
			int version, 
			Flows flows,  
			List<NetworkInfo> network, 
			FlowVisualizer initializerVisualizer,
			List<IntConsumer> undeployConsumers,
			List<IntConsumer> pauseConsumers,
			List<IntConsumer> resumeConsumers,
			List<StatusProvider> statusProviders) {
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
		this.statusProviders.addAll(statusProviders);
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

	public List<StatusReport> status() {
		// TODO: the app should not be running these, the manager should. the app should have no state about itself
		return statusProviders.stream().map(StatusProvider::report).collect(toList());
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
