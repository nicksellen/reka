package reka.net.common.sockets;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.net.ChannelAttrs;
import reka.net.ChannelAttrs.AttributeContainsMatcher;
import reka.net.NetManager;
import reka.util.StringWithVars;

public class SocketTagSendConfigurer implements OperationConfigurer {
	
	private final NetManager server;
	
	private Function<Data,String> tagFn;	
	private Function<Data,String> messageFn;
	
	public SocketTagSendConfigurer(NetManager server) {
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
		
		ops.add("tag/send", () -> {
			return new SocketBroadcastWithMatcherOperation(server, messageFn, data -> new AttributeContainsMatcher<>(ChannelAttrs.tags, tagFn.apply(data)));
		});
	}
	
}