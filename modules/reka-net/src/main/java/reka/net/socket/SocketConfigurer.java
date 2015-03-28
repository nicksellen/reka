package reka.net.socket;

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
import java.util.function.Function;

import reka.Identity;
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
import reka.net.NetServerManager.SocketFlows;
import reka.net.NetSettings;
import reka.net.common.sockets.SocketBroadcastConfigurer;
import reka.net.common.sockets.SocketSendConfigurer;
import reka.net.common.sockets.SocketStatusProvider;
import reka.net.common.sockets.SocketTagAddConfigurer;
import reka.net.common.sockets.SocketTagRemoveConfigurer;
import reka.net.common.sockets.SocketTagSendConfigurer;
import reka.net.common.sockets.Sockets;

public class SocketConfigurer extends ModuleConfigurer {
	
	private final List<ConfigBody> onConnect = new ArrayList<>();
	private final List<ConfigBody> onMessage = new ArrayList<>();
	private final List<ConfigBody> onDisconnect = new ArrayList<>();
	
	private final List<Integer> ports = new ArrayList<>();
	
	private final NetServerManager server;
	
	public SocketConfigurer(NetServerManager server) {
		this.server = server;
	}

	@Conf.Each("listen")
	public void listen(String port) {
		ports.add(Integer.valueOf(port));
	}
	
	@Conf.Each("on")
	public void main(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "connect":
			onConnect.add(config.body());
			break;
		case "message":
			onMessage.add(config.body());
			break;
		case "disconnect":
			onDisconnect.add(config.body());
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
	@Override
	public void setup(ModuleSetup app) {
		
		app.defineOperation(path("send"), provider -> new SocketSendConfigurer(server));
		app.defineOperation(path("broadcast"), provider -> new SocketBroadcastConfigurer(server));
		app.defineOperation(slashes("tag"), provider -> new SocketTagAddConfigurer(server));
		app.defineOperation(slashes("tag/add"), provider -> new SocketTagAddConfigurer(server));
		app.defineOperation(slashes("tag/rm"), provider -> new SocketTagRemoveConfigurer(server));
		app.defineOperation(slashes("tag/send"), provider -> new SocketTagSendConfigurer(server));
		
		app.registerStatusProvider(ctx -> new SocketStatusProvider(server, ctx.get(Sockets.IDENTITY)));
		
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> triggers = new HashMap<>();
		
		IdentityKey<Flow> connect = IdentityKey.named("on connect");
		IdentityKey<Flow> message = IdentityKey.named("on message");
		IdentityKey<Flow> disconnect = IdentityKey.named("on disconnect");
		
		if (!onConnect.isEmpty()) {
			triggers.put(connect, combine(onConnect));
		}
		if (!onDisconnect.isEmpty()) {
			triggers.put(disconnect, combine(onDisconnect));
		}
		if (!onMessage.isEmpty()) {
			triggers.put(message, combine(onMessage));
		}	
		
		Identity identity = Identity.create("websocket");
		
		app.onDeploy(init -> {
			init.run("set http settings", ctx -> {
				// FIXME: hackety hack, don't look back, these aren't the real HTTP settings!
				//ctx.put(Sockets.SETTINGS, NetSettings.socket(ports.get(0), null, -1));
				ctx.put(Sockets.IDENTITY, identity);
			});
		});
		
		for (int port : ports) {
			app.requirePort(port);
		}
		
		app.buildFlows(triggers, reg -> {
			
			for (int port : ports) {
				
				app.registerComponent(server.deploySocket(identity, port, new SocketFlows(reg.flowFor(connect),
																						reg.flowFor(message),
																						reg.flowFor(disconnect))));
				app.registerNetwork(port, "socket");
			}
		});
		
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

}
