package reka.net.websockets;

import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Path.path;
import static reka.util.Path.slashes;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.flow.Flow;
import reka.flow.builder.TriggerHelper;
import reka.identity.IdentityKey;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.net.NetManager;
import reka.net.NetManager.SocketFlows;
import reka.net.NetSettings;
import reka.net.NetSettings.TlsSettings;
import reka.net.NetSettings.Type;
import reka.net.common.sockets.NetStatusProvider;
import reka.net.common.sockets.SocketBroadcastConfigurer;
import reka.net.common.sockets.SocketSendConfigurer;
import reka.net.common.sockets.SocketTagAddConfigurer;
import reka.net.common.sockets.SocketTagRemoveConfigurer;
import reka.net.common.sockets.SocketTagSendConfigurer;
import reka.net.http.HostAndPort;
import reka.net.http.TlsConfigurer;

public class WebsocketConfigurer extends ModuleConfigurer {

	private static final IdentityKey<Flow> CONNECT = IdentityKey.named("on connect");
	private static final IdentityKey<Flow> DISCONNECT = IdentityKey.named("on disconnect");
	private static final IdentityKey<Flow> MESSAGE = IdentityKey.named("on message");
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	private final List<HostAndPort> listens = new ArrayList<>();
	
	private TlsSettings ssl;
	
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
		ssl = configure(new TlsConfigurer(), config).build();
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
	public void setup(AppSetup app) {
		
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
		
		app.registerStatusProvider(() -> new NetStatusProvider(net, app.identity(), NetSettings.Type.WEBSOCKET));
		
		app.buildFlows(triggers.build(), reg -> {
			for (HostAndPort listen : listens) {
			
				String host = listen.host();
				int port = listen.port();
				
				if (port == -1) {
					port = ssl != null ? 443 : 80;
				}
			
				if (ssl != null) {
					app.registerComponent(net.deployWebsocketSsl(app.identity(), new HostAndPort(host, port), ssl, 
							new SocketFlows(reg.lookup(CONNECT),reg.lookup(MESSAGE),reg.lookup(DISCONNECT))));
				} else {
					app.registerComponent(net.deployWebsocket(app.identity(), new HostAndPort(host, port), 
							new SocketFlows(reg.lookup(CONNECT),reg.lookup(MESSAGE),reg.lookup(DISCONNECT))));
				}
				
				app.registerNetwork(listen.port(), Type.WEBSOCKET.protocolString(ssl != null), details -> {
					details.putString("host", listen.host());
				});
			}
			
		});
		
		
	}
	
}