package reka.http;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.http.configurers.HttpContentConfigurer;
import reka.http.configurers.HttpRedirectConfigurer;
import reka.http.configurers.HttpRequestConfigurer;
import reka.http.configurers.HttpRouterConfigurer;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;
import reka.http.server.HttpSettings.Type;
import reka.nashorn.OperationConfigurer;

public class HttpModule extends ModuleConfigurer {

	// listen 8080
	// listen localhost:500
	// listen boo.com
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	@SuppressWarnings("unused") // haven't implemented this yet
	private BasicAuthConfigurer auth;
	
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
	private final List<Function<ConfigurerProvider,OperationConfigurer>> requestHandlers = new ArrayList<>();
	
	public HttpModule(HttpServerManager server) {
		this.server = server;
	}
	
	@Conf.Each("listen")
	public void listen(String val) {
		String host = null;
		int port = 80;
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
	
	@Conf.At("auth")
	public void auth(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "basic":
			auth = configure(new BasicAuthConfigurer(), config.body());
			break;
		default:
			throw runtime("unknown auth method %s", config.valueAsString());
		}
	}
	
	public static class BasicAuthConfigurer {
		
		private final List<Entry<String,String>> accounts = new ArrayList<>();
		
		@Conf.Config
		public void accounts(Config config) {
			for (Config c : config.body()) {
				String username = c.key();
				String password = c.valueAsString();
				accounts.add(createEntry(username, password));
			}
		}
		
	}

	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "request":
			requestHandlers.add(provider -> configure(new SequenceConfigurer(provider), config.body()));
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
	@Conf.At("sessions")
	public void sessions(Config config) {
		configureModule(new HttpSessionsModule(), config);
	}

	@Override
	public void setup(ModuleSetup module) {
		
		module.operation(path("router"), provider -> new HttpRouterConfigurer(provider));
		module.operation(path("redirect"), provider -> new HttpRedirectConfigurer());
		module.operation(path("content"), provider -> new HttpContentConfigurer());
		module.operation(path("request"), provider -> new HttpRequestConfigurer(server.nettyEventGroup()));
		module.operation(path("req"), provider -> new HttpRequestConfigurer(server.nettyEventGroup()));
		
		for (Function<ConfigurerProvider, OperationConfigurer> h : requestHandlers) {
			
			module.trigger("on request", h, registration -> {
				
				for (HostAndPort listen : listens) {
					
					final String host = listen.host() == null ? "*" : listen.host();
					final int port = listen.port();
					
					String identity = format("%s/%s/%s/http", registration.applicationIdentity(), host, port);
				
					HttpSettings settings = HttpSettings.http(port, host, Type.HTTP, registration.applicationVersion());
					
					server.deployHttp(identity, registration.flow(), settings);
					
					registration.undeploy(version -> server.undeploy(identity, version));
					registration.pause(version -> server.pause(identity, version));
					registration.resume(version -> server.resume(identity, version));
					
					registration.network(port, settings.isSsl() ? "https" : "http", MutableMemoryData.create(details -> {
						details.putString("host", host);
					}).immutable());
				
				}
			});
		}
		
	}

}
