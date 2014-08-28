package reka.http;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.parallel;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.SimpleDeployedResource;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.util.StringWithVars;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;
import reka.http.server.HttpSettings.Type;

public class WebsocketsModule extends ModuleConfigurer {
	

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
	
	private final List<HostAndPort> listens = new ArrayList<>();
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final HttpServerManager server;
	
	// TODO: have a better way to pass data around
	private final AtomicReference<HttpSettings> httpSettingsRef = new AtomicReference<>();
	
	public WebsocketsModule(HttpServerManager server) {
		this.server = server;
	}
	
	private final List<ConfigBody> onConnect = new ArrayList<>();
	private final List<ConfigBody> onDisconnect = new ArrayList<>();
	private final List<ConfigBody> onMessage = new ArrayList<>();


	@Conf.Each("listen")
	public void listen(String val) {
		checkConfig(listens.isEmpty(), "can only add one listen for websockets");
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
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		checkConfig(config.hasBody(), "must have a body");
		switch (config.valueAsString()) {
		case "connect":
			onConnect.add(config.body());
			break;
		case "disconnect":
			onDisconnect.add(config.body());
			break;
		case "message":
			onMessage.add(config.body());
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	private static Function<ConfigurerProvider,Supplier<FlowSegment>> combine(List<ConfigBody> bodies) {
		return (provider) -> {
			return () -> parallel(par -> {
				bodies.forEach(body -> {
					par.add(configure(new SequenceConfigurer(provider), body).get());
				});
			});
		};
	}
	
	@Override
	public void setup(ModuleSetup module) {
		
		module.operation(path("send"), () -> new WebsocketSendConfigurer());
		module.operation(path("broadcast"), () -> new WebsocketBroadcastConfigurer());
		
		Map<String,Function<ConfigurerProvider, Supplier<FlowSegment>>> triggers = new HashMap<>();
		
		if (!onConnect.isEmpty()) {
			triggers.put("connect", combine(onConnect));
		}
		if (!onDisconnect.isEmpty()) {
			triggers.put("disconnect", combine(onDisconnect));
		}
		if (!onMessage.isEmpty()) {
			triggers.put("message", combine(onMessage));
		}
		
		httpSettingsRef.set(HttpSettings.http(listens.get(0).port, listens.get(0).host, Type.WEBSOCKET, -1)); // FIXME: hackety hack
		
		module.triggers(triggers, registration -> {
			
			for (HostAndPort listen : listens) {
			
				final String host = listen.host() == null ? "*" : listen.host();
				final int port = listen.port();
				
				String identity = format("%s/%s/%s", registration.applicationIdentity(), host, port);
			
				HttpSettings settings = HttpSettings.http(port, host, Type.WEBSOCKET, registration.applicationVersion());
				
				server.deployWebsocket(identity, settings, handlers -> {
					
					if (registration.has("connect")) { 
						handlers.connect(registration.get("connect"));
					}
					if (registration.has("disconnect")) {
						handlers.disconnect(registration.get("disconnect"));
					}
					if (registration.has("message")) {
						handlers.message(registration.get("message"));
					}
					
				});
				
				registration.resource(new SimpleDeployedResource() {
					
					@Override
					public void undeploy(int version) {
						server.undeployWebsocket(identity, version);
					}
				});
				
			}
		});
	}
	
	/*

	public class WebsocketsTriggerConfigurer implements TriggerConfigurer {

		private final List<String> validEvents = asList("connect", "disconnect", "message");
		
		private String host;
		private int port;
		
		private final List<Path> onConnect = new ArrayList<>();
		private final List<Path> onDisconnect = new ArrayList<>();
		private final List<Path> onMessage = new ArrayList<>();
		
		@Conf.Val
		@Conf.At("listen")
		public void listen(String listen) {
			Iterator<String> split = Splitter.on(":").split(listen).iterator();
			host = split.next();
			if (split.hasNext()) {
				port = Integer.parseInt(split.next());
			}
		}
		
		@Conf.Each("on")
		public void on(Config config) {
			
			checkConfig(config.hasValue(), "must have a value");
			
			String event = config.valueAsString();
			checkConfig(validEvents.contains(event), "event must be one of %s", validEvents);

			checkConfig(config.hasBody(), "must have body");
			
			Optional<Config> o = config.body().at("run");
			checkConfig(o.isPresent(), "body must contain a 'run' option");
			
			Config body = o.get();
			checkConfig(body.hasValue(), "run must contain a value");
			
			Path run = Path.path(body.valueAsString());
			
			switch (event) {
			case "connect": onConnect.add(run); break;
			case "disconnect": onDisconnect.add(run); break;
			case "message": onMessage.add(run); break;
			}
		}
		
		
		@Override
		public void setupTriggers(TriggerSetup trigger) {

			HttpSettings settings = HttpSettings.http(port, host, Type.WEBSOCKET, trigger.applicationVersion());
			
			log.debug("setting http settings ref");
			httpSettingsRef.set(settings);
			
			trigger.requiresFlows(onConnect);
			trigger.requiresFlows(onDisconnect);
			trigger.requiresFlows(onMessage);
			
			String identity = format("ws/%s/%s/%s", trigger.identity(), host, port);
			
			trigger.addRegistrationHandler(app -> {
				
				app.protocol(port, "ws", MutableMemoryData.create()
						.putString("host", host)
					.immutable());
				
				server.deployWebsocket(identity, settings, deploy -> {
					
					for (Path e : onConnect) {
						deploy.connect(app.flows().flow(e));
					}
					
					for (Path e : onDisconnect) {
						deploy.disconnect(app.flows().flow(e));
					}
					
					for (Path e : onMessage) {
						deploy.message(app.flows().flow(e));
					}
					
				});
				
			});
			
		}
		
	}
	*/
	

	public class WebsocketSendConfigurer implements Supplier<FlowSegment> {

		private Function<Data,String> to;
		private Function<Data,String> messageFn;
		
		@Conf.At("to")
		public void to(String val) {
			to = StringWithVars.compile(val);
		}
		
		@Conf.At("message")
		public void message(String val) {
			messageFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("msg", () -> new WebsocketSendOperation(to, messageFn));
		}
		
	}


	public class WebsocketBroadcastConfigurer implements Supplier<FlowSegment> {
		
		private Function<Data,String> messageFn;
		
		@Conf.Val
		@Conf.At("message")
		public void message(String val) {
			messageFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("broadcast", () -> new WebsocketBroadcastOperation(messageFn));
		}
		
	}
	
	public class WebsocketSendOperation implements SyncOperation {

		private final Function<Data,String> toFn;
		private final Function<Data,String> messageFn;
		private final HttpSettings settings;
		
		public WebsocketSendOperation(Function<Data,String> toFn, Function<Data,String> messageFn) {
			this.toFn = toFn;
			this.messageFn = messageFn;
			this.settings = httpSettingsRef.get();
			log.debug("creating send operation");
		}
		
		@Override
		public MutableData call(MutableData data) {
			log.debug("preparing send: {}:{}", settings.host(), settings.port());
			server.sendWebsocket(settings, toFn.apply(data), messageFn.apply(data));
			return data;
		}
		
	}
	
	public class WebsocketBroadcastOperation implements SyncOperation {

		private final Function<Data,String> messageFn;
		private final HttpSettings settings;
		
		public WebsocketBroadcastOperation(Function<Data,String> messageFn) {
			log.debug("creating broadcast operation");
			this.messageFn = messageFn;
			this.settings = httpSettingsRef.get();
			log.debug(".. with settings: {}:{}", settings.host(), settings.port());
		}
		
		@Override
		public MutableData call(MutableData data) {
			log.debug("preparing broadcast: {}:{}", settings.host(), settings.port());
			String message = messageFn.apply(data);
			log.debug("running broadcast: [{}] {}:{}", message, settings.host(), settings.port());
			server.broadcastWebsocket(settings, message);
			return data;
		}
		
	}
	
	
}