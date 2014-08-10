package reka.http;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.DeployedResource;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.TriggerSetup;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
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

	// listen 8080
	// listen localhost:500
	// listen boo.com
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	public class HostAndPort {
		private final String host;
		private final int port;
		public HostAndPort(String host, int port) {
			this.host = host;
			this.port = port;
		}
		public String host() {
			return host;
		}
		public int port() {
			return port;
		}
	}

	private final HttpServerManager server;
	
	private final List<HostAndPort> listens = new ArrayList<>();
	private final List<Function<ConfigurerProvider,Supplier<FlowSegment>>> requestHandlers = new ArrayList<>();
	
	private SslSettings sslSettings;
	
	@Conf.At("ssl")
	public void ssl(Config config) {
		sslSettings = configure(new SslConfigurer(), config).build();
	}
	
	public UseHTTP(HttpServerManager server) {
		this.server = server;
	}
	
	@Conf.Each("listen")
	public void listen(String val) {
		String host = null;
		int port = -1;
		if (listenPortOnly.matcher(val).matches()) {
			port = Integer.valueOf(val);
		} else {
			Matcher m = listenHostAndPort.matcher(val);
			if (m.matches()) {
				host = m.group(1);
				port = Integer.valueOf(m.group(2));
			} else {
				host = val;
			}
		}
		listens.add(new HostAndPort(host, port));
	}

	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "request":
			requestHandlers.add((provider) -> configure(new SequenceConfigurer(provider), config.body()));
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	@Override
	public void setup(UseInit http) {
		
		http.operation("router", (provider) -> new HttpRouterConfigurer(provider));
		http.operation("redirect", () -> new HttpRedirectConfigurer());
		http.operation("content", () -> new HttpContentConfigurer());
		http.operation(asList("request", "req"), () -> new HttpRequestConfigurer(server.group()));
		http.trigger(root(), () -> new HTTPTriggerConfigurer());
		
		for (Function<ConfigurerProvider, Supplier<FlowSegment>> h : requestHandlers) {
			
			http.trigger2("request", h, register -> {
				
				for (HostAndPort listen : listens) {
					
					final String host = listen.host() == null ? "*" : listen.host();
					final int port = listen.port() == -1 ? (sslSettings != null ? 443 : 80) : listen.port();
					
					String identity = format("%s/%s/%s", register.identity(), host, port);
				
					HttpSettings settings;
					
					if (sslSettings != null) {
						settings = HttpSettings.https(port, host, Type.HTTP, sslSettings, register.applicationVersion());
					} else {
						settings = HttpSettings.http(port, host, Type.HTTP, register.applicationVersion());
					}
					
					server.deployHttp(identity, register.flow(), settings);
					
					register.resource(new DeployedResource() {
						
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
					
					register.network(port, settings.isSsl() ? "https" : "http", MutableMemoryData.create((details) -> {
						details.putString("host", host);
						details.putString("run", register.flow().name().last().toString());
					}).immutable());
				
				}
			});
		}
		
	}
	
	public class HTTPTriggerConfigurer implements TriggerConfigurer, ErrorReporter {

		private Path flowName;
		
		private final List<HostAndPort> listens = new ArrayList<>();

		private SslSettings sslSettings;
		
		@Conf.At("ssl")
		public void ssl(Config config) {
			sslSettings = configure(new SslConfigurer(), config).build();
		}
		
		@Conf.Each("listen")
		public void listen(String val) {
			String host = null;
			int port = -1;
			if (listenPortOnly.matcher(val).matches()) {
				port = Integer.valueOf(val);
			} else {
				Matcher m = listenHostAndPort.matcher(val);
				if (m.matches()) {
					host = m.group(1);
					port = Integer.valueOf(m.group(2));
				} else {
					host = val;
				}
			}
			listens.add(new HostAndPort(host, port));
		}
		
		@Conf.Each("on")
		public void flowName(Config config) {
			checkConfig(config.hasValue(), "on can be 'request'");
			switch (config.valueAsString()) {
			case "request":
				flowName = Path.path(config.body().at("run").get().valueAsString());
				break;
			default:
				throw runtime("unknown trigger %s", config.valueAsString());
			}
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(flowName, "[on request / run] option is required");
		}

		@Override
		public void setupTriggers(TriggerSetup trigger) {
			
			trigger.addRegistrationHandler(register -> {
				
				Flow flow = register.flows().flow(flowName);
				checkNotNull(flow, "flow %s was not defined", flowName);
				
				for (HostAndPort listen : listens) {
					
					final String host = listen.host() == null ? "*" : listen.host();
					final int port = listen.port() == -1 ? (sslSettings != null ? 443 : 80) : listen.port();
					
					String identity = format("http/%s/%s/%s", trigger.identity(), host, port);
				
					HttpSettings settings;
					
					if (sslSettings != null) {
						settings = HttpSettings.https(port, host, Type.HTTP, sslSettings, trigger.applicationVersion());
					} else {
						settings = HttpSettings.http(port, host, Type.HTTP, trigger.applicationVersion());
					}
					
					server.deployHttp(identity, flow, settings);
					
					register.resource(new DeployedResource() {
						
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
					
					register.protocol(port, settings.isSsl() ? "https" : "http", MutableMemoryData.create((details) -> {
						details.putString("host", host);
						details.putString("run", flowName.last().toString());
					}).immutable());
				
				}
				
			});
			
		}
	}
	
	public class HTTPTriggerConfigurerOriginal implements TriggerConfigurer, ErrorReporter {

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
