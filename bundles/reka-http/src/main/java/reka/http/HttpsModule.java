package reka.http;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.DeployedResource;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;
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

public class HttpsModule extends ModuleConfigurer implements ErrorReporter {
	
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
	
	private byte[] crt;
	private byte[] key;
	
	public HttpsModule(HttpServerManager server) {
		this.server = server;
	}
	
	@Conf.Each("listen")
	public void listen(String val) {
		String host = null;
		int port = 443;
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
	
	private static File byteToFile(byte[] bytes) {
		try {
			java.nio.file.Path tmp = Files.createTempFile("reka.", "");
			Files.write(tmp, bytes);
			return tmp.toFile();
		} catch (IOException e) {
			throw unchecked(e);
		}
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
	public void setup(ModuleSetup http) {
		
		SslSettings sslSettings = new SslSettings(byteToFile(crt), byteToFile(key));
		
		http.operation(path("router"), (provider) -> new HttpRouterConfigurer(provider));
		http.operation(path("redirect"), () -> new HttpRedirectConfigurer());
		http.operation(path("content"), () -> new HttpContentConfigurer());
		http.operation(asList(path("request"), path("req")), () -> new HttpRequestConfigurer(server.group()));
		
		for (Function<ConfigurerProvider, Supplier<FlowSegment>> h : requestHandlers) {
			
			http.trigger("https request", h, registration -> {
				
				for (HostAndPort listen : listens) {
					
					final String host = listen.host() == null ? "*" : listen.host();
					final int port = listen.port();
					
					String identity = format("%s/%s/%s/https", registration.applicationIdentity(), host, port);
				
					HttpSettings settings = HttpSettings.https(port, host, Type.HTTP, sslSettings, registration.applicationVersion());
					
					server.deployHttp(identity, registration.flow(), settings);
					
					registration.resource(new DeployedResource() {
						
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
					
					registration.network(port, "https", MutableMemoryData.create((details) -> {
						details.putString("host", host);
						details.putString("run", registration.flow().name().last().toString());
					}).immutable());
				
				}
			});
		}
		
	}

	@Override
	public void errors(ErrorCollector errors) {
		errors.checkConfigPresent(key, "key is required");
		errors.checkConfigPresent(crt, "crt is required");
	}

}
