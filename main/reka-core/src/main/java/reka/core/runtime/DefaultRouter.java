package reka.core.runtime;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import reka.api.run.RouteCollector;

public class DefaultRouter implements RouteCollector {
	
	public static DefaultRouter create(Collection<String> nodes) {
		return new DefaultRouter(nodes);
	}
	
	private final Collection<String> nodes;
	private final Collection<String> routed = new HashSet<>();
	
	private String defaultRoute;
	
	DefaultRouter(Collection<String> nodes) {
		this.nodes = nodes;
	}
	
	@Override
    public RouteCollector routeTo(String name) {
		checkArgument(nodes.contains(name), "cannot route to %s", name);
		routed.add(name);
		return this;
	}
	
	@Override
    public Collection<String> routed() {
		if (routed.isEmpty() && defaultRoute != null) {
			return Collections.singleton(defaultRoute);
		} else {
			return routed;
		}
	}
	
	@Override
    public void routeToAll() {
		routed.addAll(nodes);
	}

	@Override
	public void defaultRoute(String name) {
		defaultRoute = name;
	}
	
}