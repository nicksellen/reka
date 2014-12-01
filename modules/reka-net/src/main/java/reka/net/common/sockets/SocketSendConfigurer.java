package reka.net.common.sockets;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.net.NetServerManager;

public class SocketSendConfigurer implements OperationConfigurer {

	private final NetServerManager server;
	
	private Function<Data,String> to;
	private Function<Data,String> messageFn;
	
	public SocketSendConfigurer(NetServerManager server) {
		this.server = server;
	}
	
	@Conf.At("to")
	public void to(String val) {
		to = StringWithVars.compile(val);
	}
	
	@Conf.At("message")
	public void message(String val) {
		messageFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("msg", store -> new SocketSendOperation(server, store.get(Sockets.SETTINGS), to, messageFn));
	}
	
}