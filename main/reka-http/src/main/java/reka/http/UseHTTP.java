package reka.http;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.configurer.Configurer.configure;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

import reka.DeployedResource;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.configurer.Configurer.ErrorCollector;
import reka.configurer.ErrorReporter;
import reka.configurer.annotations.Conf;
import reka.core.bundle.TriggerSetup;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;
import reka.http.configurers.HttpContentConfigurer;
import reka.http.configurers.HttpRedirectConfigurer;
import reka.http.configurers.HttpRequestConfigurer;
import reka.http.configurers.HttpRouterConfigurer;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;
import reka.http.server.HttpSettings.SslSettings;
import reka.http.server.HttpSettings.Type;

import com.google.common.base.Splitter;

public class UseHTTP extends UseConfigurer {

	private final HttpServerManager server;
	
	public UseHTTP(HttpServerManager server) {
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

		private SslSettings sslSettings;
		
		@Conf.Val
		@Conf.At("listen")
		public void listen(String listen) {
			Iterator<String> split = Splitter.on(":").split(listen).iterator();
			host = split.next();
			if (split.hasNext()) {
				port = Integer.parseInt(split.next());
			}
		}
		
		@Conf.At("ssl")
		public void ssl(Config config) {
			sslSettings = configure(new SslConfigurer(), config).build();
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
		public void setupTriggers(TriggerSetup trigger) {

			if (port == -1) {
				port = sslSettings != null ? 443 : 80;
			}
			
			trigger.addRegistrationHandler(app -> {
				
				checkNotNull(flowName, "export http: please specify which flow to run (from %s)", app.flows().names());
				Flow flow = app.flows().flow(flowName);
				checkNotNull(flow, "flow %s was not defined", flowName);
				
				String identity = format("http/%s/%s/%s", trigger.identity(), host, port);
				
				HttpSettings settings;
				
				if (sslSettings != null) {
					settings = HttpSettings.https(port, host, Type.HTTP, sslSettings, trigger.applicationVersion());
				} else {
					settings = HttpSettings.http(port, host, Type.HTTP, trigger.applicationVersion());
				}
				
				server.deployHttp(identity, flow, settings);
				
				app.resource(new DeployedResource() {
					
					@Override
					public void undeploy(int version) {
						server.undeploy(identity, version);	
					}
					
					@Override
					public void pause(int version) {
						server.pause(identity, version);
					}

					
					@Override
					public void resume(int version) {
						server.resume(identity, version);
					}
					
				});
				
				app.protocol(port, settings.isSsl() ? "https" : "http", MutableMemoryData.create((details) -> {
					details.putString("host", host);
					details.putString("run", flowName.last().toString());
				}).immutable());
				
			});
			
		}
	}
	
	public static class SslConfigurer {
		
		private byte[] crt;
		private byte[] key;
		
		@Conf.At("crt")
		public void crt(Config val) {
			checkConfig(val.hasDocument(), "must have document!");
			crt = val.documentContent();
		}
		
		@Conf.At("key")
		public void key(Config val) {
			checkConfig(val.hasDocument(), "must have document!");
			key = val.documentContent();
		}
		
		SslSettings build() {
			return new SslSettings(byteToFile(crt), byteToFile(key));
		}
		
		private static File byteToFile(byte[] bytes) {
			try {
				java.nio.file.Path tmp = Files.createTempFile("reka.", "");
				Files.write(tmp, bytes);
				return tmp.toFile();
			} catch (IOException e) {
				throw unchecked(e);
			}
		}
		
	}

}
