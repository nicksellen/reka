package reka.net.common.sockets;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketSendOperation implements Operation {

	private final NetServerManager server;
	private final Function<Data,String> toFn;
	private final Function<Data,String> messageFn;
	private final NetSettings settings;
	
	public SocketSendOperation(NetServerManager server, NetSettings settings, Function<Data,String> toFn, Function<Data,String> messageFn) {
		this.server = server;
		this.toFn = toFn;
		this.messageFn = messageFn;
		this.settings = settings;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channel(settings, toFn.apply(data), channel -> {
			channel.writeAndFlush(messageFn.apply(data));
		});
	}
	
}