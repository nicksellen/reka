package reka.net.http.configurers;

import static reka.api.Path.dots;
import static reka.config.configurer.Configurer.configure;
import io.netty.handler.codec.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.run.RouteKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.dirs.AppDirs;
import reka.net.http.operations.HttpRouter;
import reka.net.http.operations.HttpRouter.HttpRouteVar;

import com.google.common.collect.ImmutableList;

public class HttpRouterConfigurer extends HttpRouteGroupConfigurer implements OperationConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(HttpRouterConfigurer.class);

	private final ConfigurerProvider provider;
    
    public HttpRouterConfigurer(AppDirs dirs, ConfigurerProvider provider) {
    	super(dirs, provider);
        this.provider = provider;
    }
    
	static final Logger logger = LoggerFactory.getLogger("http-router-builder");
	
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
		private RouteKey key;

		public RouteBuilder method(String value) {
			method = HttpMethod.valueOf(value);
			return this;
		}
		
		public RouteBuilder path(String value) {
			path = value;
			return this;
		}

		public RouteBuilder key(RouteKey value) {
			key = value;
			return this;
		}
		
		public HttpRouter.Route build() {
			
			List<HttpRouteVar> vars = new ArrayList<>();
			
			StringBuffer regex = new StringBuffer();
			
			regex.append("^");
			
			if (!path.startsWith("/")) {
				regex.append("\\/");
			}
			
			Matcher matcher = PATH_VAR.matcher(path);

			int pos = 0;
			String var;
			
			int matchGroupId = 1;
			
			while (matcher.find()) {
				
				var = matcher.group(1);
				
				if (var == null) {
					// a {path} kind of key, just remove the '{' and '}'
					var = matcher.group(2);
					var = var.substring(1, var.length() - 1);
				}

				boolean starred = false;
				boolean optional = false;
				
				if (var.endsWith("?")) {
					optional = true;
					var = var.substring(0, var.length() - 1);
				}
				
				if (var.endsWith("*")) {
					starred = true;
					var = var.substring(0, var.length() - 1);
				}
				
				vars.add(new HttpRouteVar(matchGroupId++, dots(var)));

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

			if (key == null) {
				throw new RuntimeException("to node was null :(");
			}
			
			if (vars.isEmpty()) {
				return new HttpRouter.StaticRoute(key, path, method, RouteFormatters.create(path));
			} else {
				log.debug("http router regex [{}] for [{}]", pattern, key);
				return new HttpRouter.RegexRoute(key, pattern, ImmutableList.copyOf(vars), method, RouteFormatters.create(path));
			}
		}

	}

	@Override
	public void setup(OperationSetup ops) {
		HttpRouter router = new HttpRouter(buildGroupRoutes(), missing != null);
		
		ops.router("router", store -> router, routes -> {
			routes.parallel(par -> buildGroupSegment(par));
			if (missing != null) {
				routes.add(HttpRouter.OTHERWISE, missing);
			}
		});
		
	}
	
}
