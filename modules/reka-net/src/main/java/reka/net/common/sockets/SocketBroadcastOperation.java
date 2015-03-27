package reka.net.common.sockets;

import java.util.function.Function;

import reka.Identity;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.NetServerManager;

public class SocketBroadcastOperation implements Operation {

	private final NetServerManager server;
	private final Function<Data,String> messageFn;
	private final Identity identity;
	
	public SocketBroadcastOperation(NetServerManager server, Identity identity, Function<Data,String> messageFn) {
		this.server = server;
		this.messageFn = messageFn;
		this.identity = identity;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channels(identity).writeAndFlush(messageFn.apply(data));
	}
	
}