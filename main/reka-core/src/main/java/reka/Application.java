package reka;

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.setup.NetworkInfo;
import reka.core.setup.StatusProvider;

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
	private final Map<Path,String> moduleVersions = new LinkedHashMap<>();
	
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
			List<StatusProvider> statusProviders,
			Map<Path,String> moduleVersions) {
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
		this.statusProviders.add(new ApplicationStatusProvider());
		this.network.sort(naturalOrder());
		//this.moduleVersions.putAll(moduleVersions);
		
		List<Path> moduleKeys = new ArrayList<>(moduleVersions.keySet());
		Collections.sort(moduleKeys);
		moduleKeys.forEach(key -> this.moduleVersions.put(key, moduleVersions.get(key)));
	}
	
	private class ApplicationStatusProvider implements StatusProvider {

		@Override
		public String name() {
			return "app";
		}
		
		@Override
		public String version() {
			return "unknown";
		}
		
		@Override
		public boolean up() {
			return true; // the application itself is always up
		}

		@Override
		public void statusData(MutableData data) {
			data.putList("flows", list -> {
				flows.all().forEach(flow -> {
					list.addMap(m -> {
						m.putString("name", flow.name().slashes());
						m.putLong("requests", flow.stats().requests.sum());
						m.putLong("errors", flow.stats().errors.sum());
						m.putLong("halts", flow.stats().halts.sum());
					});
				});
			});
			/*
			data.putMap("modules", map -> {
				moduleVersions.forEach((key, version) -> {
					map.putString(key.slashes(), version);
				});
			});
			*/
			
			
		}

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
	
	public List<StatusProvider> statusProviders() {
		return statusProviders;
	}
	
	public Map<Path,String> moduleVersions() {
		return moduleVersions;
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
