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
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.config.ConfigurerProvider;
import reka.core.config.NoOp;
import reka.core.config.SequenceConfigurer;
import reka.http.configurers.HttpRouterConfigurer.RouteBuilder;
import reka.http.operations.HttpRouter;
import reka.http.operations.HttpRouter.Route;
import reka.nashorn.OperationsConfigurer;

public class HttpRouteGroupConfigurer {

	private final ConfigurerProvider provider;

	private final Map<String, List<OperationsConfigurer>> segments = new HashMap<>();
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

			for (Entry<String, List<OperationsConfigurer>> entry : segments.entrySet()) {
				
				String connectionName = entry.getKey();
				List<OperationsConfigurer> operations = entry.getValue();
				
				par.routeSeq(connectionName, route -> {
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

		OperationsConfigurer segment = configToSegment(config);
		
		if (!segments.containsKey(route.connectionName())) {
			segments.put(route.connectionName(), new ArrayList<>());
		}

		segments.get(route.connectionName()).add(segment);
		
	}
	
	private OperationsConfigurer configToSegment(Config config) {
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
		String connectionName = format("%s %s", method, pattern);
		addRoute(new RouteBuilder().method(method).path(pattern).connectionName(connectionName).build(), config);
	}

}