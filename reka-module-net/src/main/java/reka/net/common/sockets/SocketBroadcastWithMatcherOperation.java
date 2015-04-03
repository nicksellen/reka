package reka.net.common.sockets;

import io.netty.channel.group.ChannelMatcher;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.net.NetManager;

public class SocketBroadcastWithMatcherOperation implements Operation {

	private final NetManager server;
	private final Function<Data,String> messageFn;
	private final Function<Data,ChannelMatcher> matcherFn;
	
	public SocketBroadcastWithMatcherOperation(NetManager server, Function<Data,String> messageFn, Function<Data,ChannelMatcher> matcherFn) {
		this.server = server;
		this.messageFn = messageFn;
		this.matcherFn = matcherFn;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channels(matcherFn.apply(data)).writeAndFlush(messageFn.apply(data));
	}
	
}