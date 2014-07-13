package reka.http;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.sync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.TriggerSetup;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;
import reka.core.util.StringWithVars;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;
import reka.http.server.HttpSettings.Type;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

public class UseWebsockets extends UseConfigurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final HttpServerManager server;
	
	// TODO: have a better way to pass data around
	private final AtomicReference<HttpSettings> httpSettingsRef = new AtomicReference<>();
	
	public UseWebsockets(HttpServerManager server) {
		this.server = server;
	}

	@Override
	public void setup(UseInit init) {
		init.operation("send", () -> new WebsocketSendConfigurer());
		init.operation("broadcast", () -> new WebsocketBroadcastConfigurer());
		init.trigger(root(), () -> new WebsocketsTriggerConfigurer());
	}

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
					.readonly());
				
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