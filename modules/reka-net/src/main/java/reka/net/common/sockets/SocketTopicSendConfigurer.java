package reka.net.common.sockets;

import static java.lang.String.format;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.net.NetServerManager;

public class SocketTopicSendConfigurer implements OperationConfigurer {
	
	private final NetServerManager server;
	private final String topic;
	
	private Function<Data,String> messageFn;
	
	public SocketTopicSendConfigurer(NetServerManager server, String topic) {
		this.server = server;
		this.topic = topic;
	}
	
	@Conf.Val
	@Conf.At("message")
	public void message(String val) {
		messageFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add(format("%s/send", topic), store -> new SocketTopicSendOperation(server, store.get(Sockets.SETTINGS), topic, messageFn));
	}
	
}