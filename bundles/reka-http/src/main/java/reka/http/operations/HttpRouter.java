package reka.http.operations;

import static java.lang.String.format;
import static reka.api.Path.dots;
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
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hasher;

public class HttpRouter implements RouterOperation {
	
	public static final RouteKey ELSE = RouteKey.named("else");
	
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

	private final Set<RouteKey> connectionNames = new HashSet<>();
	private final Collection<Route> routes;
	private final boolean hasElseRoute;

	public HttpRouter(Collection<Route> incoming, boolean hasElseRoute) {
		
		List<Route> sortedRoutes = new ArrayList<>(incoming);
		
		Collections.sort(sortedRoutes, routeComparator);
		
		this.routes = ImmutableList.copyOf(sortedRoutes);
		this.hasElseRoute = hasElseRoute;
		
		for (Route route : routes) {
			log.debug("route: {}", route.key());
			connectionNames.add(route.key());
		}
		
		if (hasElseRoute) {
			connectionNames.add(ELSE);
		}
		
	}
		
	@Override
	public void call(MutableData data, RouteCollector router) {
		
		String path = data.getString(Request.PATH).orElse("");
		HttpMethod method = HttpMethod.valueOf(data.getString(Request.METHOD).orElse("GET"));
		
		for (Route route : routes) {
			if (route.matches(method, path, data)) {
				router.routeTo(route.key());
				break;
			}
		}

		if (hasElseRoute && router.routed().isEmpty()) {
			router.routeTo(ELSE);
		}
		
	}
	
	public static interface Route {
		public RouteKey key();
		public boolean matches(HttpMethod method, String path, MutableData data);
		public RouteFormatter formatter();
	}
	
	private static final Map<Class<? extends Route>,Integer> orderings = new HashMap<>();
	
	static {
		orderings.put(MountRoute.class, 10);
		orderings.put(StaticRoute.class, 20);
		orderings.put(RegexRoute.class, 30);
	}
	
	private static final Comparator<Route> routeComparator = new Comparator<HttpRouter.Route>() {

		@Override
		public int compare(Route a, Route b) {
			if (a.getClass().equals(b.getClass())) {
				if (a instanceof StaticRoute) {
					return compareStaticRoutes((StaticRoute) a, (StaticRoute) b);
				} else if (a instanceof MountRoute) {
					return compareMountRoutes((MountRoute) a, (MountRoute) b);
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

		private int compareMountRoutes(MountRoute a, MountRoute b) {
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
	
	
	public static final class MountRoute implements Route {
		
		private final RouteKey key;
		private final String prefix;
		private final String prefixWithTrailingSlash;
		
		public MountRoute(String prefix) {
			if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
			this.prefix = prefix;
			this.prefixWithTrailingSlash = prefix + '/';
			key = RouteKey.named(format("prefix %s", prefix));
		}

		@Override
		public RouteKey key() {
			return key;
		}

		@Override
		public boolean matches(HttpMethod method, String path, MutableData data) {
			if (path.equals(prefix)) {
				data.putString(Request.PATH_BASE, prefix)
					.putString(Request.PATH, "/");
				return true;
			} else if (path.startsWith(prefixWithTrailingSlash)) {
				data.putString(Request.PATH_BASE, prefix)
					.putString(Request.PATH, path.substring(prefix.length()));
				return true;
			} else {
				return false;
			}
		}

		@Override
		public RouteFormatter formatter() {
			throw unsupported();
		}
		
	}
	
	public static final class StaticRoute implements Route {

		private final RouteKey key;
		private final String path;
		private final HttpMethod method;

		private final RouteFormatter formatter;

		public StaticRoute(RouteKey key, String path, HttpMethod method, RouteFormatter formatter) {
			this.key = key;
			this.path = path;
			this.method = method;
			this.formatter = formatter;
		}

		@Override
		public String toString() {
			return format("<%s connection=%s path=%s method=%s>",
					getClass().getSimpleName(), key, path, method);
		}

		@Override
		public RouteKey key() {
			return key;
		}

		@Override
		public boolean matches(HttpMethod method, String path, MutableData data) {
			return method.equals(this.method) && this.path.equals(path);
		}

		@Override
		public RouteFormatter formatter() {
			return formatter;
		}
	}
	
	public static final class HttpRouteVar {
		private final int id;
		private final Path path;
		public HttpRouteVar(int id, Path path) {
			this.id = id;
			this.path = path;
		}
	}
	
	public static final class RegexRoute implements Route {
		
		private final RouteKey key;
		private final Pattern pattern;
		private final List<HttpRouteVar> keys;
		private final HttpMethod method;
		private final RouteFormatter formatter;
		
		private final ThreadLocal<Matcher> m = new ThreadLocal<Matcher>(){

			@Override
			protected Matcher initialValue() {
				return pattern.matcher("");
			}
			
		};

		public RegexRoute(RouteKey key, Pattern pattern, List<HttpRouteVar> keys,
				HttpMethod method, RouteFormatter formatter) {
			this.key = key;
			this.pattern = pattern;
			this.keys = keys;
			this.method = method;
			this.formatter = formatter;
		}
		
		@Override
		public String toString() {
			return format("<%s connection=%s pattern=%s method=%s>", getClass().getSimpleName(), key, pattern, method);
		}

		@Override
		public RouteKey key() {
			return key;
		}
		
		@Override
		public boolean matches(HttpMethod method, String path, MutableData data) {
			if (method.equals(this.method)) {
				Matcher matcher = m.get().reset(path);
				if (matcher.matches()) {
					for (HttpRouteVar key : keys) {
						String value = matcher.group(key.id);
						if (value != null) {
							data.putString(key.path, value);
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
