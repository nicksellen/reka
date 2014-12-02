package reka.net.common.sockets;

import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.ChannelMatchers;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.net.ChannelAttrs.ChannelIdMatcher;
import reka.net.NetServerManager;

public class SocketBroadcastConfigurer implements OperationConfigurer {
	
	private final NetServerManager server;
	private Function<Data,String> messageFn;
	private Function<Data,ChannelMatcher> matcherFn;
	
	public SocketBroadcastConfigurer(NetServerManager server) {
		this.server = server;			
	}
	
	@Conf.Val
	@Conf.At("message")
	public void message(String val) {
		messageFn = StringWithVars.compile(val);
	}
	
	@Conf.At("exclude")
	public void exclude(String val) {
		StringWithVars idFn = StringWithVars.compile(val);
		if (idFn.hasVariables()) {
			matcherFn = (data) -> ChannelMatchers.invert(new ChannelIdMatcher(idFn.apply(data)));	
		} else {
			matcherFn = (data) -> ChannelMatchers.invert(new ChannelIdMatcher(idFn.original()));
		}
	}
	
	@Override
	public void setup(OperationSetup ops) {
		if (matcherFn != null) {
			ops.add("broadcast", store -> new SocketBroadcastWithMatcherOperation(server, store.get(Sockets.SETTINGS), messageFn, matcherFn));
		} else {
			ops.add("broadcast", store -> new SocketBroadcastOperation(server, store.get(Sockets.SETTINGS), messageFn));
		}
	}
	
}