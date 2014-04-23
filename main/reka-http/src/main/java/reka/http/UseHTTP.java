package reka.http;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.root;

import java.util.Iterator;

import reka.api.Path;
import reka.api.flow.Flow;
import reka.configurer.Configurer.ErrorCollector;
import reka.configurer.ErrorReporter;
import reka.configurer.annotations.Conf;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.SetupTrigger;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;
import reka.http.configurers.HttpContentConfigurer;
import reka.http.configurers.HttpRedirectConfigurer;
import reka.http.configurers.HttpRequestConfigurer;
import reka.http.configurers.HttpRouterConfigurer;
import reka.http.server.HttpServer;
import reka.http.server.HttpSettings;
import reka.http.server.HttpSettings.Security;
import reka.http.server.HttpSettings.Type;

import com.google.common.base.Splitter;

public class UseHTTP extends UseConfigurer {

	private final HttpServer server;
	
	public UseHTTP(HttpServer server) {
		this.server = server;
	}

	@Override
	public void setup(UseInit http) {
		http.operation("router", (provider) -> new HttpRouterConfigurer(provider));
		http.operation("redirect", () -> new HttpRedirectConfigurer());
		http.operation("content", () -> new HttpContentConfigurer());
		http.operation(asList("request", "req"), () -> new HttpRequestConfigurer(server.group()));
		http.trigger(root(), () -> new HTTPTriggerConfigurer());
	}
	
	public class HTTPTriggerConfigurer implements TriggerConfigurer, ErrorReporter {

		private String host;
		private int port = -1;
		private Path flowName;
		
		@Conf.Val
		@Conf.At("listen")
		public void listen(String listen) {
			Iterator<String> split = Splitter.on(":").split(listen).iterator();
			host = split.next();
			if (split.hasNext()) {
				port = Integer.parseInt(split.next());
			}
		}
		
		@Conf.At("host")
		public void host(String val) {
			host = val;
		}
		
		@Conf.At("port")
		public void host(Integer val) {
			port = val;
		}
		
		@Conf.At("run")
		public void flowName(String val) {
			flowName = Path.path(val);
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(flowName, "[run] option is required");
		}

		@Override
		public void setupTriggers(SetupTrigger trigger) {
			
			trigger.onStart(app -> {
				
				checkNotNull(flowName, "export http: please specify which flow to run (from %s)", app.flows().names());
				Flow flow = app.flows().flow(flowName);
				checkNotNull(flow, "flow %s was not defined", flowName);
				
				String identity = format("http/%s/%s/%s", trigger.identity(), host, port);
				
				HttpSettings settings = new HttpSettings(port, host, Type.HTTP, Security.NONE);
				server.deployHttp(identity, flow, settings);

				app.onUndeploy((v) -> {
					server.undeploy(identity);
				});
				
				app.register(port, "http", MutableMemoryData.create((details) -> {
					details.putString("host", host);
					details.putString("run", flowName.last().toString());
				}).readonly());
				
			});
			
		}
	}


}
