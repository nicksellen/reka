package reka.net.common.sockets;

import io.netty.channel.group.ChannelMatcher;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.net.ChannelAttrs.ChannelTagMatcher;
import reka.net.NetServerManager;

public class SocketTagSendConfigurer implements OperationConfigurer {
	
	private final NetServerManager server;
	
	private Function<Data,String> tagFn;	
	private Function<Data,String> messageFn;
	
	public SocketTagSendConfigurer(NetServerManager server) {
		this.server = server;
	}
	
	@Conf.At("tag")
	public void tag(String val) {
		tagFn = StringWithVars.compile(val);
	}
	
	@Conf.At("message")
	public void message(String val) {
		messageFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("tag/send", store -> {
			Function<Data,ChannelMatcher> fn = data -> new ChannelTagMatcher(tagFn.apply(data));
			return new SocketBroadcastWithMatcherOperation(server, store.get(Sockets.SETTINGS), messageFn, fn);
		});
	}
	
}