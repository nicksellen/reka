package reka.http.operations;

import static java.lang.String.format;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.util.Util.runtime;
import static reka.util.Util.unsupported;
import io.netty.handler.codec.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Hashable;
import reka.api.Path;
import reka.api.Path.Request;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hasher;

public class HttpRouter implements RoutingOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("http-router");
	
	public static interface RouteFormatter extends Hashable {
		public String url(Map<String, Object> parameters);
		public String url(Object... parameters);
		public String url();
	}

	@SuppressWarnings("unused")
	private static final Path ROUTE_FORMATTER_PATH = dots("route");

	private final Set<String> connectionNames = new HashSet<>();
	private final Collection<Route> routes;
	private final String notFoundConnectionName;

	public HttpRouter(Collection<Route> incoming, String notFoundConnectionName) {
		
		List<Route> sortedRoutes = new ArrayList<>(incoming);
		
		Collections.sort(sortedRoutes, routeComparator);
		
		this.routes = ImmutableList.copyOf(sortedRoutes);
		this.notFoundConnectionName = notFoundConnectionName;
		
		for (Route route : routes) {
			log.debug("route: {}", route.name());
			connectionNames.add(route.connectionName());
		}
		
		if (notFoundConnectionName != null) {
			connectionNames.add(notFoundConnectionName);
		}
		
	}
		
	@Override
	public MutableData call(MutableData data, RouteCollector router) {
		
		String path = data.getString(Request.PATH).orElse("");
		String contentType = data.getString(Request.Headers.CONTENT_TYPE).orElse("");
		HttpMethod method = HttpMethod.valueOf(data.getString(Request.METHOD).orElse("GET"));
		
		for (Route route : routes) {
			if (route.matches(method, contentType, path, data)) {
				router.routeTo(route.connectionName());
				break;
			}
		}

		if (notFoundConnectionName != null && router.routed().isEmpty()) {
			router.routeTo(notFoundConnectionName);
		}
		
		/* TODO: need to add route formatters into a 'helpers' type thing 
		for (Entry<String, RouteFormatter> entry : namedRouteFormatters.entrySet()) {
			data.put(ROUTE_FORMATTER_PATH.add(entry.getKey()), new RouteFunctions(entry.getValue(), matchedRoutes.contains(entry.getKey())));
		}
		*/
		
		return data;
	}
	
	public static interface Route {
		public String connectionName();
		public boolean matches(HttpMethod method, String contentType, String path, MutableData params);
		public String name();
		public RouteFormatter formatter();
	}
	
	private static final Map<Class<? extends Route>,Integer> orderings = new HashMap<>();
	
	static {
		orderings.put(PrefixRoute.class, 10);
		orderings.put(StaticRoute.class, 20);
		orderings.put(RegexRoute.class, 30);
	}
	
	private static final Comparator<Route> routeComparator = new Comparator<HttpRouter.Route>() {

		@Override
		public int compare(Route a, Route b) {
			if (a.getClass().equals(b.getClass())) {
				if (a instanceof StaticRoute) {
					return compareStaticRoutes((StaticRoute) a, (StaticRoute) b);
				} else if (a instanceof PrefixRoute) {
					return comparePrefixRoutes((PrefixRoute) a, (PrefixRoute) b);
				} else if (a instanceof RegexRoute) {
					return compareRegexRoutes((RegexRoute) a, (RegexRoute) b);
				} else {
					throw runtime("don't know how to compare [%s]", a.getClass());
				}
			} else {
				return Integer.compare(orderings.getOrDefault(a.getClass(), 50), 
									   orderings.getOrDefault(b.getClass(), 50));
			}
		}
		
		private int compareStaticRoutes(StaticRoute a, StaticRoute b) {
			if (a.method != b.method) {
				return a.method.compareTo(b.method);
			} else {
				return Integer.compare(a.path.length(), b.path.length());
			}
		}

		private int comparePrefixRoutes(PrefixRoute a, PrefixRoute b) {
			return a.prefix.compareTo(b.prefix);
		}
		
		private int compareRegexRoutes(RegexRoute a, RegexRoute b) {
			if (a.keys.size() != b.keys.size()) {
				return Integer.compare(b.keys.size(), a.keys.size());
			} else if (!a.pattern.pattern().equals(b.pattern.pattern())) {
				return Integer.compare(
						b.pattern.pattern().length(),
						a.pattern.pattern().length());
			} else if (!a.method.name().equals(b.method.name())){
				return a.method.name().compareTo(b.method.name());
			} else {
				return 0;
			}
		}
		
	};
	
	
	public static final class PrefixRoute implements Route {
		
		private final String prefix;
		private final String prefixWithTrailingSlash;
		
		public PrefixRoute(String prefix) {
			if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
			this.prefix = prefix;
			this.prefixWithTrailingSlash = prefix + '/';
		}

		@Override
		public String connectionName() {
			return format("prefix %s", prefix);
		}

		@Override
		public boolean matches(HttpMethod method, String contentType, String path, MutableData params) {
			return path.equals(prefix) || path.startsWith(prefixWithTrailingSlash);
		}

		@Override
		public String name() {
			return connectionName();
		}

		@Override
		public RouteFormatter formatter() {
			throw unsupported();
		}
		
	}
	
	public static final class StaticRoute implements Route {

		private final String connectionName;
		private final String path;
		private final HttpMethod method;
		private final String name;

		private final RouteFormatter formatter;

		public StaticRoute(String connectionName, String path,
				HttpMethod method, String name, RouteFormatter formatter) {
			this.connectionName = connectionName;
			this.path = path;
			this.method = method;
			this.name = name;
			this.formatter = formatter;
		}

		@Override
		public String toString() {
			return format("<%s connection=%s path=%s method=%s>",
					getClass().getSimpleName(), connectionName, path, method);
		}

		@Override
		public String connectionName() {
			return connectionName;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean matches(HttpMethod method, String contentType, String path, MutableData params) {
			return method.equals(this.method) && this.path.equals(path);
		}

		@Override
		public RouteFormatter formatter() {
			return formatter;
		}
	}
	
	public static final class RegexRoute implements Route {
		
		private final String connectionName;
		private final Pattern pattern;
		private final Map<String, String> keys;
		private final HttpMethod method;
		private final String name;
		private final RouteFormatter formatter;

		public RegexRoute(String connectionName, Pattern pattern, Map<String, String> keys,
				HttpMethod method, String name, RouteFormatter formatter) {
			this.connectionName = connectionName;
			this.pattern = pattern;
			this.keys = keys;
			this.method = method;
			this.name = name;
			this.formatter = formatter;
		}
		
		@Override
		public String toString() {
			return format("<%s connection=%s pattern=%s method=%s>", getClass().getSimpleName(), connectionName, pattern, method);
		}

		@Override
		public String connectionName() {
			return connectionName;
		}
		
		@Override
		public String name() {
			return name;
		}
		
		@Override
		public boolean matches(HttpMethod method, String contentType, String path, MutableData params) {
			if (method.equals(this.method)) {
				Matcher matcher = pattern.matcher(path);
				if (matcher.matches()) {
					for (Entry<String, String> key : keys.entrySet()) {
						String value = matcher.group(key.getKey());
						if (value != null) {
							params.putString(path(key.getValue()), value);
						}
					}
					return true;
				}
			}
			return false;
		}
		
		@Override
		public RouteFormatter formatter() {
			return formatter;
		}
		
	}
	
	public static class RouteFunctions implements RouteFormatter {
		
		private final RouteFormatter formatter;
		private final boolean isCurrent;
		
		RouteFunctions(RouteFormatter formatter, boolean isCurrent) {
			this.formatter = formatter;
			this.isCurrent = isCurrent;
		}
		
		@Override
		public String url(Map<String, Object> parameters) {
			return formatter.url(parameters);
		}
		
		@Override
		public String url(Object... parameters) {
			return formatter.url(parameters);
		}
		
		@Override
		public String url() {
			return formatter.url();
		}
		
		public boolean isCurrent() {
			return isCurrent;
		}
		
		@Override
		public Hasher hash(Hasher hasher) {
			return formatter.hash(hasher).putBoolean(isCurrent);
		}
		
	}

}
