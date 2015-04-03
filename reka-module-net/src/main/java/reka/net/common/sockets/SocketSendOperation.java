package reka.net.common.sockets;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.identity.Identity;
import reka.net.ChannelAttrs;
import reka.net.NetManager;

public class SocketSendOperation implements Operation {

	private final NetManager server;
	private final Function<Data,String> toFn;
	private final Function<Data,String> messageFn;
	private final Identity identity;
	
	public SocketSendOperation(NetManager server, Identity identity, Function<Data,String> toFn, Function<Data,String> messageFn) {
		this.server = server;
		this.toFn = toFn;
		this.messageFn = messageFn;
		this.identity = identity;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channels(identity).withAttr(ChannelAttrs.id, toFn.apply(data)).writeAndFlush(messageFn.apply(data));
	}
	
}