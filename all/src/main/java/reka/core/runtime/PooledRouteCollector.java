package reka.core.runtime;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import reka.api.run.RouteCollector;
import reka.api.run.RouteKey;
import reka.util.Recycler;
import reka.util.Recycler.Handle;

public class PooledRouteCollector implements RouteCollector {
	  
	private static final Recycler<PooledRouteCollector> RECYCLER = new Recycler<PooledRouteCollector>() {
		@Override
		protected PooledRouteCollector newObject(Handle handle) {
			return new PooledRouteCollector(handle);
		}
	};

	public static PooledRouteCollector get(Collection<RouteKey> all) {
		PooledRouteCollector collector = RECYCLER.get();
		collector.all = all;
		return collector;
	}

	private final Handle handle;
	
	private Collection<RouteKey> all;
	private Collection<RouteKey> routed = new HashSet<>();
	
	private List<RouteKey> defaultKey = new ArrayList<>(1);
	
	private PooledRouteCollector(Handle handle) {
		this.handle = handle;
		defaultKey.clear();
	}
	
    public boolean recycle() {
    	all = null;
		routed.clear();
		defaultKey.clear();
        return RECYCLER.recycle(this, handle);
    }
	
	@Override
    public RouteCollector routeTo(RouteKey key) {
		checkArgument(all.contains(key), "cannot route to %s, we just have %s", key, all);
		routed.add(key);
		return this;
	}
	
	@Override
    public Collection<RouteKey> routed() {
		return routed.isEmpty() ? defaultKey : routed;
	}
	
	@Override
    public void routeToAll() {
		routed.addAll(all);
	}

	@Override
	public void defaultRoute(RouteKey key) {
		defaultKey.add(key);
	}
	
}