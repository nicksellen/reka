package reka.http;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.parallel;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.runtime;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

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

import reka.api.IdentityKey;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
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

public class WebsocketModule extends ModuleConfigurer {
	

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
	
	public WebsocketModule(HttpServerManager server) {
		this.server = server;
	}
	
	private final List<ConfigBody> onConnect = new ArrayList<>();
	private final List<ConfigBody> onDisconnect = new ArrayList<>();
	private final List<ConfigBody> onMessage = new ArrayList<>();
	
	private final List<WebsocketTopicConfigurer> topics = new ArrayList<>();

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
	
	@Conf.Each("topic")
	public void topic(Config config) {
		topics.add(configure(new WebsocketTopicConfigurer(IdentityKey.of(config.valueAsString())), config.body()));
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
		
		topics.forEach(topic -> {
			module.operation(asList(path(topic.key().name()), path(topic.key().name(), "send")), () -> new WebsocketTopicSendConfigurer(topic.key()));
			module.operation(path(topic.key().name(), "subscribe"), () -> new WebsocketTopicSubscribeConfigurer(topic.key()));
		});
		
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, Supplier<FlowSegment>>> triggers = new HashMap<>();
		
		IdentityKey<Flow> connect = IdentityKey.of("connect");
		IdentityKey<Flow> disconnect = IdentityKey.of("disconnect");
		IdentityKey<Flow> message = IdentityKey.of("message");
		
		if (!onConnect.isEmpty()) {
			triggers.put(connect, combine(onConnect));
		}
		if (!onDisconnect.isEmpty()) {
			triggers.put(disconnect, combine(onDisconnect));
		}
		if (!onMessage.isEmpty()) {
			triggers.put(message, combine(onMessage));
		}
		
		if (listens.isEmpty()) return;
		
		httpSettingsRef.set(HttpSettings.http(listens.get(0).port, listens.get(0).host, Type.WEBSOCKET, -1)); // FIXME: hackety hack
		
		module.triggers(triggers, registration -> {
			
			for (HostAndPort listen : listens) {
			
				final String host = listen.host() == null ? "*" : listen.host();
				final int port = listen.port();
				
				String identity = format("%s/%s/%s/ws", registration.applicationIdentity(), host, port);
			
				HttpSettings settings = HttpSettings.http(port, host, Type.WEBSOCKET, registration.applicationVersion());
				
				server.deployWebsocket(identity, settings, ws -> {
					
					topics.forEach(topic -> {
						ws.topic(topic.key());
					});
					
					if (registration.has(connect)) { 
						ws.connect(registration.get(connect));
					}
					
					if (registration.has(disconnect)) {
						ws.disconnect(registration.get(disconnect));
					}
					
					if (registration.has(message)) {
						ws.message(registration.get(message));
					}
					
				});
				
				registration.undeploy(version -> server.undeploy(identity, version));
				
			}
		});
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
			return sync("broadcast", (store) -> new WebsocketBroadcastOperation(messageFn));
		}
		
	}

	public class WebsocketTopicSendConfigurer implements Supplier<FlowSegment> {
		
		private final IdentityKey<Object> key;
		
		private Function<Data,String> messageFn;
		
		public WebsocketTopicSendConfigurer(IdentityKey<Object> key) {
			this.key = key;
		}
		
		@Conf.Val
		@Conf.At("message")
		public void message(String val) {
			messageFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync(format("%s/send", key.name()), (store) -> new WebsocketTopicSendOperation(key, messageFn));
		}
		
	}

	public class WebsocketTopicSubscribeConfigurer implements Supplier<FlowSegment> {
		
		private final IdentityKey<Object> key;
		
		private Function<Data,String> idFn;
		
		public WebsocketTopicSubscribeConfigurer(IdentityKey<Object> key) {
			this.key = key;
		}
		
		@Conf.Val
		@Conf.At("id")
		public void message(String val) {
			idFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync(format("%s/subscribe", key.name()), () -> new WebsocketTopicSubscribeOperation(key, idFn));
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
			server.websocket(settings, ws -> {
				ws.channel(toFn.apply(data)).ifPresent(channel -> {
					if (channel.isOpen()) {
						channel.writeAndFlush(new TextWebSocketFrame(messageFn.apply(data)));
					}
				});
			});
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
			log.debug("running broadcast: {}:{}", settings.host(), settings.port());
			server.websocket(settings, ws -> {
				ws.channels.values().forEach(channel -> {
					if (channel.isOpen()) {
						channel.writeAndFlush(new TextWebSocketFrame(messageFn.apply(data)));
					}
				});
			});
			return data;
		}
		
	}

	public class WebsocketTopicSendOperation implements SyncOperation {

		private final IdentityKey<Object> key;
		private final Function<Data,String> messageFn;
		private final HttpSettings settings;
		
		public WebsocketTopicSendOperation(IdentityKey<Object> key, Function<Data,String> messageFn) {
			log.debug("creating broadcast operation");
			this.key = key;
			this.messageFn = messageFn;
			this.settings = httpSettingsRef.get();
			log.debug(".. with settings: {}:{}", settings.host(), settings.port());
		}
		
		@Override
		public MutableData call(MutableData data) {
			log.debug("preparing broadcast: {}:{}", settings.host(), settings.port());
			log.debug("running topic send: {}:{}", settings.host(), settings.port());
			server.websocket(settings, ws -> {
				ws.topic(key).ifPresent(topic -> {
					topic.channels().forEach(channel -> {
						if (channel.isOpen()) {
							channel.writeAndFlush(new TextWebSocketFrame(messageFn.apply(data)));	
						}
					});
				});
			});
			
			return data;
		}
		
	}
	
	public class WebsocketTopicSubscribeOperation implements SyncOperation {

		private final IdentityKey<Object> key;
		private final Function<Data,String> idFn;
		private final HttpSettings settings;
		
		public WebsocketTopicSubscribeOperation(IdentityKey<Object> key, Function<Data,String> idFn) {
			this.key = key;
			this.idFn = idFn;
			this.settings = httpSettingsRef.get();
			log.debug(".. with settings: {}:{}", settings.host(), settings.port());
		}
		
		@Override
		public MutableData call(MutableData data) {
			server.websocket(settings, ws -> {
				ws.channel(idFn.apply(data)).ifPresent(channel -> {
					if (channel.isOpen()) {
						ws.topic(key).ifPresent(topic -> {
							topic.register(channel);
						});
					}
				});
			});
			return data;
		}
		
	}
	
}