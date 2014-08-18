package reka.http.configurers;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.noop;
import static reka.core.builder.FlowSegments.sequential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.http.configurers.HttpRouterConfigurer.RouteBuilder;
import reka.http.operations.HttpRouter;
import reka.http.operations.HttpRouter.Route;

public class HttpRouteGroupConfigurer {

	private final ConfigurerProvider provider;

	private final Map<String, List<Supplier<FlowSegment>>> segments = new HashMap<>();
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
		then = configure(provider.provide("run", provider).get(), config);
	}

	protected FlowSegment buildGroupSegment() {

		FlowSegment main = sequential(seq -> {

			if (groupName != null) seq.addLabel(groupName);

			seq.parallel(par -> {

				for (Entry<String, List<Supplier<FlowSegment>>> entry : segments.entrySet()) {
					
					List<FlowSegment> segs = entry.getValue().stream().map(Supplier<FlowSegment>::get).collect(toList());
					par.add(entry.getKey(), segs);
					
				}

				for (HttpRouteGroupConfigurer group : groups) {
					par.add(group.buildGroupSegment());
				}

			});

			if (then != null) seq.add(then.get());

		});

		return main;
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

		Supplier<FlowSegment> segment = configToSegment(config);
		
		if (!segments.containsKey(route.connectionName())) {
			segments.put(route.connectionName(), new ArrayList<>());
		}

		segments.get(route.connectionName()).add(segment);
		
	}
	
	private Supplier<FlowSegment> configToSegment(Config config) {
		if (config.hasDocument()) {
			return configure(new HttpContentConfigurer(), config);
		} else if (config.hasBody()) {
			return configure(new SequenceConfigurer(provider), config);
		} else {
			return () -> noop("nothing");
		}
	}
	
	private void addPatternRoute(String method, String pattern, Config config) {
		String connectionName = format("%s %s", method, pattern);
		addRoute(new RouteBuilder().method(method).path(pattern).connectionName(connectionName).build(), config);
	}

}