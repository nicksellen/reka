package reka.http;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;
import reka.core.util.StringWithVars;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;

public class WebsocketsModule extends ModuleConfigurer {
	
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

	@Override
	public void setup(ModuleInit init) {
		init.operation(path("send"), () -> new WebsocketSendConfigurer());
		init.operation(path("broadcast"), () -> new WebsocketBroadcastConfigurer());
		
		// TODO: fixup websockets, I can't pass all these connection things into
		// it because they come back independently. I need something that lets me add in
		// handlers one at a time. and maybe something at the end to confirm we're all done...
		
		onConnect.forEach(body -> {
			init.trigger("connect", body, registration -> {
				
			});
		});
		
		onDisconnect.forEach(body -> {
			
		});
		
		onMessage.forEach(body -> {
			
		});
		
		//init.trigger(root(), () -> new WebsocketsTriggerConfigurer());
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