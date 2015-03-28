package reka.net.socket;

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

import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.core.setup.OperationConfigurer;
import reka.net.NetManager;
import reka.net.NetManager.SocketFlows;
import reka.net.NetSettings;
import reka.net.common.sockets.SocketBroadcastConfigurer;
import reka.net.common.sockets.SocketSendConfigurer;
import reka.net.common.sockets.NetStatusProvider;
import reka.net.common.sockets.SocketTagAddConfigurer;
import reka.net.common.sockets.SocketTagRemoveConfigurer;
import reka.net.common.sockets.SocketTagSendConfigurer;

public class SocketConfigurer extends ModuleConfigurer {
	
	private final List<ConfigBody> onConnect = new ArrayList<>();
	private final List<ConfigBody> onMessage = new ArrayList<>();
	private final List<ConfigBody> onDisconnect = new ArrayList<>();
	
	private final List<Integer> ports = new ArrayList<>();
	
	private final NetManager net;
	
	public SocketConfigurer(NetManager net) {
		this.net = net;
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
	public void setup(AppSetup app) {
		
		app.defineOperation(path("send"), provider -> new SocketSendConfigurer(net));
		app.defineOperation(path("broadcast"), provider -> new SocketBroadcastConfigurer(net));
		app.defineOperation(slashes("tag"), provider -> new SocketTagAddConfigurer(net));
		app.defineOperation(slashes("tag/add"), provider -> new SocketTagAddConfigurer(net));
		app.defineOperation(slashes("tag/rm"), provider -> new SocketTagRemoveConfigurer(net));
		app.defineOperation(slashes("tag/send"), provider -> new SocketTagSendConfigurer(net));
		
		app.registerStatusProvider(() -> new NetStatusProvider(net, app.identity(), NetSettings.Type.SOCKET));
		
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
		
		for (int port : ports) {
			app.requireNetwork(port);
		}
		
		app.buildFlows(triggers, flows -> {
			for (int port : ports) {
				app.registerComponent(net.deploySocket(app.identity(), port, new SocketFlows(flows.lookup(connect),
																							 flows.lookup(message),
																							 flows.lookup(disconnect))));
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
