package reka.net.websockets;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.TriggerHelper;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.ModuleSetupContext;
import reka.net.NetManager;
import reka.net.NetManager.SocketFlows;
import reka.net.NetSettings.SslSettings;
import reka.net.NetSettings.Type;
import reka.net.common.sockets.SocketBroadcastConfigurer;
import reka.net.common.sockets.SocketSendConfigurer;
import reka.net.common.sockets.SocketStatusProvider;
import reka.net.common.sockets.SocketTagAddConfigurer;
import reka.net.common.sockets.SocketTagRemoveConfigurer;
import reka.net.common.sockets.SocketTagSendConfigurer;
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
	
	private final NetManager net;
	
	public WebsocketConfigurer(NetManager net) {
		this.net = net;
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
		
		ModuleSetupContext ctx = app.ctx();
		
		listens.replaceAll(listen -> listen.port() == -1 ? new HostAndPort(listen.host(), ssl != null ? 443 : 80) : listen);
		
		app.defineOperation(path("send"), provider -> new SocketSendConfigurer(net));
		app.defineOperation(path("broadcast"), provider -> new SocketBroadcastConfigurer(net));
		app.defineOperation(slashes("tag/add"), provider -> new SocketTagAddConfigurer(net));
		app.defineOperation(slashes("tag/rm"), provider -> new SocketTagRemoveConfigurer(net));
		app.defineOperation(slashes("tag/send"), provider -> new SocketTagSendConfigurer(net));
		
		if (listens.isEmpty()) return;
		
		listens.forEach(listen -> {
			app.requireNetwork(listen.port(), listen.host());	
		});
		
		app.registerStatusProvider(() -> new SocketStatusProvider(net, app.identity()));
		
		app.buildFlows(triggers.build(), reg -> {
			for (HostAndPort listen : listens) {
			
				String host = listen.host();
				int port = listen.port();
				
				if (port == -1) {
					port = ssl != null ? 443 : 80;
				}
			
				if (ssl != null) {
					app.registerComponent(net.deployWebsocketSsl(app.identity(), new HostAndPort(host, port), ssl, 
							new SocketFlows(reg.flowFor(CONNECT),reg.flowFor(MESSAGE),reg.flowFor(DISCONNECT))));
				} else {
					app.registerComponent(net.deployWebsocket(app.identity(), new HostAndPort(host, port), 
							new SocketFlows(reg.flowFor(CONNECT),reg.flowFor(MESSAGE),reg.flowFor(DISCONNECT))));
				}
				
				app.registerNetwork(listen.port(), Type.WEBSOCKET.protocolString(ssl != null), details -> {
					details.putString("host", listen.host());
				});
			}
			
		});
		
		
	}
	
}