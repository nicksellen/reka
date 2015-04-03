package reka.net.common.sockets;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.identity.Identity;
import reka.net.NetManager;

public class SocketBroadcastOperation implements Operation {

	private final NetManager server;
	private final Function<Data,String> messageFn;
	private final Identity identity;
	
	public SocketBroadcastOperation(NetManager server, Identity identity, Function<Data,String> messageFn) {
		this.server = server;
		this.messageFn = messageFn;
		this.identity = identity;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channels(identity).writeAndFlush(messageFn.apply(data));
	}
	
}