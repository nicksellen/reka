package reka.net.websockets;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationConfigurer;
import reka.net.NetServerManager;
import reka.net.NetSettings;
import reka.net.NetSettings.SslSettings;
import reka.net.common.sockets.SocketBroadcastConfigurer;
import reka.net.common.sockets.SocketSendConfigurer;
import reka.net.common.sockets.SocketStatusProvider;
import reka.net.common.sockets.SocketTagAddConfigurer;
import reka.net.common.sockets.SocketTagRemoveConfigurer;
import reka.net.common.sockets.SocketTagSendConfigurer;
import reka.net.common.sockets.Sockets;
import reka.net.http.HostAndPort;
import reka.net.http.SslConfigurer;

public class WebsocketConfigurer extends ModuleConfigurer {
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	private final List<HostAndPort> listens = new ArrayList<>();
	
	private SslSettings ssl;
	
	private final NetServerManager server;
	
	public WebsocketConfigurer(NetServerManager server) {
		this.server = server;
	}
	
	private final List<ConfigBody> onConnect = new ArrayList<>();
	private final List<ConfigBody> onDisconnect = new ArrayList<>();
	private final List<ConfigBody> onMessage = new ArrayList<>();

	@Conf.Each("listen")
	public void listen(String val) {
		checkConfig(listens.isEmpty(), "can only add one listen for websockets");
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
	
	@Conf.At("ssl")
	public void ssl(Config config) {
		ssl = configure(new SslConfigurer(), config).build();
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

	private static Function<ConfigurerProvider,OperationConfigurer> combine(List<ConfigBody> bodies) {
		return provider -> {
			return ops -> {
				ops.parallel(par -> {
					bodies.forEach(body -> {
						par.add(configure(new SequenceConfigurer(provider), body));
					});
				});
			};
		};
	}
	
	@Override
	public void setup(ModuleSetup module) {
		
		listens.replaceAll(listen -> listen.port() == -1 ? new HostAndPort(listen.host(), ssl != null ? 443 : 80) : listen);
		
		module.operation(path("send"), provider -> new SocketSendConfigurer(server));
		module.operation(path("broadcast"), provider -> new SocketBroadcastConfigurer(server));
		module.operation(slashes("tag/add"), provider -> new SocketTagAddConfigurer(server));
		module.operation(slashes("tag/rm"), provider -> new SocketTagRemoveConfigurer(server));
		module.operation(slashes("tag/send"), provider -> new SocketTagSendConfigurer(server));
		
		listens.forEach(listen -> {
			module.requirePort(listen.port(), Optional.of(listen.host()));	
		});
		
		module.status(store -> new SocketStatusProvider(server, store.get(Sockets.SETTINGS)));
		
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> triggers = new HashMap<>();
		
		IdentityKey<Flow> connect = IdentityKey.named("on connect");
		IdentityKey<Flow> disconnect = IdentityKey.named("on disconnect");
		IdentityKey<Flow> message = IdentityKey.named("on message");
		
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
		
		module.setupInitializer(init -> {
			init.run("set http settings", store -> {
				// FIXME: hackety hack, don't look back, these aren't the real HTTP settings!
				int port = listens.get(0).port();
				if (port == -1) {
					port = ssl != null ? 443 : 80;
				}
				store.put(Sockets.SETTINGS, NetSettings.ws(port, listens.get(0).host(), null, -1));
			});
		});
		
		module.triggers(triggers, reg -> {
			
			for (HostAndPort listen : listens) {
			
				String host = listen.host();
				int port = listen.port();
				
				if (port == -1) {
					port = ssl != null ? 443 : 80;
				}
				
				String identity = format("%s/%s/%s/ws", reg.applicationIdentity(), host, port);
			
				NetSettings settings;
				if (ssl != null) {
					settings = NetSettings.wss(port, host, reg.applicationIdentity(), reg.applicationVersion(), ssl);
				} else {
					settings = NetSettings.ws(port, host, reg.applicationIdentity(), reg.applicationVersion());
				}
				
				server.deployWebsocket(identity, settings, ws -> {
					reg.flowFor(connect).ifPresent(flow -> ws.onConnect(flow));
					reg.flowFor(disconnect).ifPresent(flow -> ws.onDisconnect(flow));
					reg.flowFor(message).ifPresent(flow -> ws.onMessage(flow));					
				});
				
				reg.network(listen.port(), settings.protocolString(), details -> {
					details.putString("host", listen.host());
				});
				
				reg.onUndeploy(version -> server.undeploy(identity, version));
				
			}
		});
	}
	
}