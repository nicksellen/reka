package reka.net.common.sockets;

import io.netty.channel.group.ChannelMatcher;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketBroadcastWithMatcherOperation implements Operation {

	private final NetServerManager server;
	private final Function<Data,String> messageFn;
	private final NetSettings settings;
	private final Function<Data,ChannelMatcher> matcherFn;
	
	public SocketBroadcastWithMatcherOperation(NetServerManager server, NetSettings settings, Function<Data,String> messageFn, Function<Data,ChannelMatcher> matcherFn) {
		this.server = server;
		this.messageFn = messageFn;
		this.settings = settings;
		this.matcherFn = matcherFn;
	}
	
	@Override
	public void call(MutableData data) {
		server.channels(settings, channels -> {
			channels.writeAndFlush(messageFn.apply(data), matcherFn.apply(data));
		});
	}
	
}