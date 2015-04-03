package reka.runtime;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;

public class DefaultRouteCollector implements RouteCollector {
	
	public static RouteCollector create(Collection<RouteKey> all) {
		return new DefaultRouteCollector(all);
	}
	
	private final Collection<RouteKey> all;
	private final Collection<RouteKey> routed = new HashSet<>();
	
	private RouteKey defaultKey;
	
	DefaultRouteCollector(Collection<RouteKey> all) {
		this.all = all;
	}
	
	@Override
    public RouteCollector routeTo(RouteKey key) {
		checkArgument(all.contains(key), "cannot route to %s, we just have %s", key, all);
		routed.add(key);
		return this;
	}
	
	@Override
    public Collection<RouteKey> routed() {
		if (routed.isEmpty() && defaultKey != null) {
			return Collections.singleton(defaultKey);
		} else {
			return routed;
		}
	}
	
	@Override
    public void routeToAll() {
		routed.addAll(all);
	}

	@Override
	public void defaultRoute(RouteKey key) {
		defaultKey = key;
	}
	
}