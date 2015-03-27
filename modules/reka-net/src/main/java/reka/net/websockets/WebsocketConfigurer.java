package reka.net.websockets;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.Identity;
import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.TriggerHelper;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.ModuleSetupContext;
import reka.net.NetServerManager;
import reka.net.NetServerManager.SocketFlows;
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

	private static final IdentityKey<Flow> CONNECT = IdentityKey.named("on connect");
	private static final IdentityKey<Flow> DISCONNECT = IdentityKey.named("on disconnect");
	private static final IdentityKey<Flow> MESSAGE = IdentityKey.named("on message");
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	private final List<HostAndPort> listens = new ArrayList<>();
	
	private SslSettings ssl;
	
	private final NetServerManager server;
	
	public WebsocketConfigurer(NetServerManager server) {
		this.server = server;
	}
	
	private final TriggerHelper triggers = new TriggerHelper();

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
			triggers.add(CONNECT, config.body());
			break;
		case "disconnect":
			triggers.add(DISCONNECT, config.body());
			break;
		case "message":
			triggers.add(MESSAGE, config.body());
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
	@Override
	public void setup(ModuleSetup app) {
		
		listens.replaceAll(listen -> listen.port() == -1 ? new HostAndPort(listen.host(), ssl != null ? 443 : 80) : listen);
		
		app.operation(path("send"), provider -> new SocketSendConfigurer(server));
		app.operation(path("broadcast"), provider -> new SocketBroadcastConfigurer(server));
		app.operation(slashes("tag/add"), provider -> new SocketTagAddConfigurer(server));
		app.operation(slashes("tag/rm"), provider -> new SocketTagRemoveConfigurer(server));
		app.operation(slashes("tag/send"), provider -> new SocketTagSendConfigurer(server));
		
		if (listens.isEmpty()) return;
		
		listens.forEach(listen -> {
			app.requirePort(listen.port(), Optional.of(listen.host()));	
		});
		
		app.status(unused -> new SocketStatusProvider(server, app.identity()));
		
		app.triggers(triggers.build(), reg -> {
			for (HostAndPort listen : listens) {
			
				String host = listen.host();
				int port = listen.port();
				
				if (port == -1) {
					port = ssl != null ? 443 : 80;
				}
			
				if (ssl != null) {
					app.register(server.deployWebsocketSsl(app.identity(), new HostAndPort(host, port), ssl, 
							new SocketFlows(reg.flowFor(CONNECT),reg.flowFor(MESSAGE),reg.flowFor(DISCONNECT))));
				} else {
					app.register(server.deployWebsocket(app.identity(), new HostAndPort(host, port), 
							new SocketFlows(reg.flowFor(CONNECT),reg.flowFor(MESSAGE),reg.flowFor(DISCONNECT))));
				}
				
				app.network(listen.port(), "ws" + ssl != null ? "s" : "", details -> {
					details.putString("host", listen.host());
				});
			}
			
		});
		
		
	}
	
}