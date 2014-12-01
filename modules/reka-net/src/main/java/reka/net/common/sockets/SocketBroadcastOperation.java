package reka.net.common.sockets;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketBroadcastOperation implements Operation {

	private final NetServerManager server;
	private final Function<Data,String> messageFn;
	private final NetSettings settings;
	
	public SocketBroadcastOperation(NetServerManager server, NetSettings settings, Function<Data,String> messageFn) {
		this.server = server;
		this.messageFn = messageFn;
		this.settings = settings;
	}
	
	@Override
	public void call(MutableData data) {
		server.channels(settings, channels -> {
			channels.writeAndFlush(messageFn.apply(data));
		});
	}
	
}