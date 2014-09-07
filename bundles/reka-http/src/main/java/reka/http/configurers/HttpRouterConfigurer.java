package reka.http.configurers;

import static reka.api.Path.dots;
import static reka.config.configurer.Configurer.configure;
import io.netty.handler.codec.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationSetup;
import reka.http.operations.HttpRouter;
import reka.http.operations.HttpRouter.RouteKey;
import reka.nashorn.OperationConfigurer;

import com.google.common.collect.ImmutableList;

public class HttpRouterConfigurer extends HttpRouteGroupConfigurer implements OperationConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(HttpRouterConfigurer.class);

	private final ConfigurerProvider provider;
    
    public HttpRouterConfigurer(ConfigurerProvider provider) {
    	super(provider);
        this.provider = provider;
    }
    
	static final Logger logger = LoggerFactory.getLogger("http-router-builder");

	private static final String missingRouteName = "notfound";
	
	private OperationConfigurer missing;
	
	/* 		examples:
	 * 			"/some/{param}/thing"
	 * 			"/something/{else*}
	 */
	//private static final Pattern BRACE_PATH_ROUTE = Pattern.compile("(?:\\{([a-zA-Z0-9_-]+\\*?\\??)\\})");
	
	/* 		examples:
	 * 			/some/:param/thing
	 * 			/something/:else
	 * 			/something/:else*
	 * 			/a/:{nested.kind.of.path}/inside/here
	 * 			/same/but/with/:{a.star.at.the.end.too}*
	 */
	private static final Pattern PATH_VAR = Pattern.compile("(?:\\:(?:([a-zA-Z0-9_\\-]+\\*?\\??)|(\\{[a-zA-Z0-9_\\-\\.]+\\}\\*?\\??)))");

	@Conf.At("missing")
	@Conf.At("otherwise")
	public void missing(Config config) {
		missing = configure(new SequenceConfigurer(provider), config);
	}
	
	public static class RouteBuilder {

		private HttpMethod method;
		private String path;
		private String connectionName;
		private String name;

		public RouteBuilder method(String value) {
			method = HttpMethod.valueOf(value);
			return this;
		}
		
		public RouteBuilder path(String value) {
			path = value;
			return this;
		}

		public RouteBuilder name(String value) {
			name = value;
			return this;
		}
		
		public RouteBuilder connectionName(String value) {
			connectionName = value;
			return this;
		}
		
		public HttpRouter.Route build() {
			
			List<RouteKey> keys = new ArrayList<>();
			
			StringBuffer regex = new StringBuffer();
			
			regex.append("^");
			
			Matcher matcher = PATH_VAR.matcher(path);

			int pos = 0;
			String key;
			
			int matchGroupId = 1;
			
			while (matcher.find()) {
				
				key = matcher.group(1);
				
				if (key == null) {
					// a {path} kind of key, just remove the '{' and '}'
					key = matcher.group(2);
					key = key.substring(1, key.length() - 1);
				}

				boolean starred = false;
				boolean optional = false;
				
				if (key.endsWith("?")) {
					optional = true;
					key = key.substring(0, key.length() - 1);
				}
				
				if (key.endsWith("*")) {
					starred = true;
					key = key.substring(0, key.length() - 1);
				}
				
				keys.add(new RouteKey(matchGroupId++, dots(key)));

				// the bit of text between the last var and this one
				String remainder = "";
				if (pos != matcher.start()) {
					remainder = path.substring(pos, matcher.start());
				}

				if (starred && matcher.end() == path.length() && remainder.endsWith("/")) {
					// a * in the last position, make the preceding '/' optional
					remainder = remainder.substring(0, remainder.length() - 1);
					regex.append(Pattern.quote(remainder)); // remainder without the trailing slash
					regex.append("\\/?"); // optional trailing slash
				} else if (!remainder.isEmpty()) {
					regex.append(Pattern.quote(remainder));
				}
				
				if (starred) {
					regex.append("(.*)");
				} else {
					regex.append("([^\\/]+)");
				}
				
				if (optional) {
					regex.append("?");
				}
				
				pos = matcher.end();
			}
			
			if (pos < path.length()) {
				regex.append(Pattern.quote(path.substring(pos)));
			}
			
			regex.append("$");

			Pattern pattern = Pattern.compile(regex.toString());

			if (connectionName == null) {
				throw new RuntimeException("to node was null :(");
			}
			
			if (name == null) {
				name = connectionName;
			}
			
			if (keys.isEmpty()) {
				return new HttpRouter.StaticRoute(connectionName, path, method, name, RouteFormatters.create(path));
			} else {
				log.debug("http router regex [{}] for [{}]", pattern, connectionName);
				return new HttpRouter.RegexRoute(connectionName, pattern, ImmutableList.copyOf(keys), method, name, RouteFormatters.create(path));
			}
		}

	}

	@Override
	public void setup(OperationSetup ops) {
			
		ops.addRouter("http/router", store -> new HttpRouter(buildGroupRoutes(), missing != null ? missingRouteName : null));
		
		ops.parallel(par -> {
			buildGroupSegment(par);
			if (missing != null) {
				par.route(missingRouteName, missing);
			}
		});
	}
	
}
