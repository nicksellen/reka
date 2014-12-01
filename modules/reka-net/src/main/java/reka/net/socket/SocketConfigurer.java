package reka.net.socket;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import reka.net.common.sockets.SocketBroadcastConfigurer;
import reka.net.common.sockets.SocketSendConfigurer;
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
	
	/*
	 *  I DID IT ALL WRONG :)
	 *  
	 *  I *do* need a single socketservermanager for all deployments
	 *  this enables the seamless redeploys
	 *  
	 *  port is probably fine as identity as you have to take over
	 *  the whole port for these.
	 * 
	 */

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
	public void setup(ModuleSetup module) {
		
		module.operation(path("send"), provider -> new SocketSendConfigurer(server));
		module.operation(path("broadcast"), provider -> new SocketBroadcastConfigurer(server));
		//module.operation(path("topic"), provider -> new SocketTopicSendConfigurer(server));
		//module.operation(path("broadcast"), provider -> new SocketBroadcastConfigurer());
	
		module.registerPortChecker(server.portChecker);
		
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> triggers = new HashMap<>();
		
		IdentityKey<Flow> connect = IdentityKey.named("connect");
		IdentityKey<Flow> disconnect = IdentityKey.named("disconnect");
		IdentityKey<Flow> message = IdentityKey.named("message");
		
		if (!onConnect.isEmpty()) {
			triggers.put(connect, combine(onConnect));
		}
		if (!onDisconnect.isEmpty()) {
			triggers.put(disconnect, combine(onDisconnect));
		}
		if (!onMessage.isEmpty()) {
			triggers.put(message, combine(onMessage));
		}	
		
		module.setupInitializer(init -> {
			init.run("set http settings", store -> {
				// FIXME: hackety hack, don't look back, these aren't the real HTTP settings!
				store.put(Sockets.SETTINGS, NetSettings.socket(ports.get(0), null, -1));
			});
		});
		
		module.triggers(triggers, reg -> {
			
			for (int port : ports) {
				
				String id = format("%s/%s", reg.applicationIdentity(), port);
				NetSettings settings = NetSettings.socket(port, reg.applicationIdentity(), reg.applicationVersion());
				
				server.deploySocket(id, settings, s -> {
					reg.flowFor(connect).ifPresent(flow -> s.onConnect(flow));
					reg.flowFor(disconnect).ifPresent(flow -> s.onDisconnect(flow));
					reg.flowFor(message).ifPresent(flow -> s.onMessage(flow));
				});
				
				reg.undeploy(version -> server.undeploy(id, version));
				
				reg.network(port, settings.isSsl() ? "socket-ssl" : "socket");
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
