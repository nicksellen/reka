package reka.net.common.sockets;

import java.util.function.Function;

import reka.Identity;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.ChannelAttrs;
import reka.net.NetServerManager;

public class SocketSendOperation implements Operation {

	private final NetServerManager server;
	private final Function<Data,String> toFn;
	private final Function<Data,String> messageFn;
	private final Identity identity;
	
	public SocketSendOperation(NetServerManager server, Identity identity, Function<Data,String> toFn, Function<Data,String> messageFn) {
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