package reka.http.configurers;

import static java.lang.String.format;
import static reka.config.configurer.Configurer.configure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.api.run.RouteKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.NoOp;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationSetup;
import reka.http.configurers.HttpRouterConfigurer.RouteBuilder;
import reka.http.operations.HttpRouter;
import reka.http.operations.HttpRouter.Route;
import reka.nashorn.OperationConfigurer;

public class HttpRouteGroupConfigurer {

	private final ConfigurerProvider provider;

	private final Map<RouteKey, List<OperationConfigurer>> segments = new HashMap<>();
	private final List<HttpRouter.Route> routes = new ArrayList<>();
	private final List<HttpRouteGroupConfigurer> groups = new ArrayList<>();

	private Supplier<FlowSegment> then;
	private String groupName;

	HttpRouteGroupConfigurer(ConfigurerProvider provider) {
		this.provider = provider;
	}

	@Conf.Val
	public void groupName(String val) {
		groupName = val;
	}

	@Conf.Each("group")
	@Conf.Each("named")
	public void group(Config config) {
		groups.add(configure(new HttpRouteGroupConfigurer(provider), config));
	}

	@Conf.Each("GET")
	public void get(Config config) {
		addPatternRoute("GET", config.valueAsString(), config);
	}

	@Conf.Each("POST")
	public void post(Config config) {
		addPatternRoute("POST", config.valueAsString(), config);
	}

	@Conf.Each("PUT")
	public void put(Config config) {
		addPatternRoute("PUT", config.valueAsString(), config);
	}

	@Conf.Each("DELETE")
	public void delete(Config config) {
		addPatternRoute("DELETE", config.valueAsString(), config);
	}
	
	@Conf.Each("prefixed")
	@Conf.Each("within")
	@Conf.Each("mount")
	public void mount(Config config) {
		addRoute(new HttpRouter.MountRoute(config.valueAsString()), config);
	}

	@Conf.At("then")
	public void then(Config config) {
		run(config);
	}

	@Conf.At("run")
	public void run(Config config) {
		then = provider.provide("run", provider, config);
	}

	protected void buildGroupSegment(OperationSetup ops) {

		if (groupName != null) ops.label(groupName);

		ops.parallel(par -> {

			for (Entry<RouteKey, List<OperationConfigurer>> entry : segments.entrySet()) {
				
				RouteKey key = entry.getKey();
				List<OperationConfigurer> operations = entry.getValue();
				
				par.namedInputSeq(key, route -> {
					operations.forEach(routeOperations -> route.add(routeOperations));
				});
			}

			for (HttpRouteGroupConfigurer group : groups) {
				par.sequential(g -> group.buildGroupSegment(g));
			}

		});

		if (then != null) ops.add(then);
		
	}

	protected List<HttpRouter.Route> buildGroupRoutes() {
		List<HttpRouter.Route> allroutes = new ArrayList<>();
		allroutes.addAll(routes);
		for (HttpRouteGroupConfigurer group : groups) {
			allroutes.addAll(group.buildGroupRoutes());
		}
		return allroutes;
	}
	
	private void addRoute(Route route, Config config) {
		
		routes.add(route);

		OperationConfigurer segment = configToSegment(config);
		
		if (!segments.containsKey(route.key())) {
			segments.put(route.key(), new ArrayList<>());
		}

		segments.get(route.key()).add(segment);
		
	}
	
	private OperationConfigurer configToSegment(Config config) {
		if (config.hasDocument()) {
			return configure(new HttpContentConfigurer(), config);
		} else if (config.hasBody()) {
			return configure(new SequenceConfigurer(provider), config);
		} else {
			// TODO: make it so we don't have to pass a NoOp here...
			return ops -> { ops.add("-", store -> NoOp.INSTANCE); };
		}
	}
	
	private void addPatternRoute(String method, String pattern, Config config) {
		RouteKey key = RouteKey.named(format("%s %s", method, pattern));
		addRoute(new RouteBuilder().method(method).path(pattern).key(key).build(), config);
	}

}