package reka.core.app;

import static java.lang.String.format;
import static java.util.Comparator.naturalOrder;
import static reka.util.Util.unchecked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.setup.NetworkInfo;
import reka.core.setup.StatusProvider;
import reka.util.AsyncShutdown;

public class Application implements AsyncShutdown {

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
		this.statusProviders.add(new ApplicationStatusProvider());
		this.network.sort(naturalOrder());
	}
	
	private class ApplicationStatusProvider implements StatusProvider {

		@Override
		public String name() {
			return "app";
		}
		
		@Override
		public String alias() {
			return "app";
		}
		
		@Override
		public String version() {
			return "core";
		}
		
		@Override
		public boolean up() {
			return true; // this should probably be looking at whether ALL the other things are up
		}

		@Override
		public void statusData(MutableData data) {
			AtomicLong totalreqs = new AtomicLong();
			AtomicLong totalerrs = new AtomicLong();
			data.putList("flows", list -> {
				flows.all().forEach(flow -> {
					list.addMap(m -> {
						m.putString("name", flow.name().slashes());
						long reqs = flow.stats().requests.sum();
						long errs = flow.stats().errors.sum();
						totalreqs.addAndGet(reqs);
						totalerrs.addAndGet(errs);
						m.putLong("requests", reqs);
						m.putLong("completed", flow.stats().completed.sum());
						m.putLong("errors", errs);
						m.putLong("halts", flow.stats().halts.sum());
					});
				});
			});
			data.putString("summary", format("calls:%d errs:%d", totalreqs.get(), totalerrs.get()));
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
	

	public void undeploy() {
		FutureResult f = AsyncShutdown.resultFuture();
		shutdown(f);
		try {
			f.future().get(10, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw unchecked(e);
		}
	}

	@Override
	public void shutdown(Result res) {
		undeployConsumers.forEach(c -> { 
			try {
				c.accept(version);
			} catch (Throwable t) {
				t.printStackTrace();
			}	
		});
		res.complete();
	}

}