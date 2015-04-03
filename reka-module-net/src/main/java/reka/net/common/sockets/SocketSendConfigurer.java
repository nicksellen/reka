package reka.net.common.sockets;

import java.util.function.Function;

import reka.app.Application;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.net.NetManager;
import reka.util.StringWithVars;

public class SocketSendConfigurer implements OperationConfigurer {

	private final NetManager server;
	
	private Function<Data,String> to;
	private Function<Data,String> messageFn;
	
	public SocketSendConfigurer(NetManager server) {
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
		ops.add("msg", () -> new SocketSendOperation(server, ops.ctx().get(Application.IDENTITY), to, messageFn));
	}
	
}