package reka.net.common.sockets;

import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.ChannelMatchers;

import java.util.function.Function;

import reka.Identity;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.net.ChannelAttrs;
import reka.net.ChannelAttrs.AttributeMatcher;
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
			matcherFn = data -> ChannelMatchers.invert(new AttributeMatcher<>(ChannelAttrs.id, idFn.apply(data)));	
		} else {
			ChannelMatcher matcher = ChannelMatchers.invert(new AttributeMatcher<>(ChannelAttrs.id, val));
			matcherFn = data -> matcher;
		}
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("broadcast", ctx -> {
			Identity identity = ctx.get(Sockets.IDENTITY);
			ChannelMatcher identityMatcher = new AttributeMatcher<>(ChannelAttrs.identity, identity);
			if (matcherFn != null) {
				matcherFn = matcherFn.andThen(m -> ChannelMatchers.compose(identityMatcher, m));
			} else {
				matcherFn = data -> identityMatcher;
			}
			return new SocketBroadcastWithMatcherOperation(server, messageFn, matcherFn);
		});
	}
	
}