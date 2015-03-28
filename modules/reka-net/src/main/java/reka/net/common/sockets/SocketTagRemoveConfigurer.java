package reka.net.common.sockets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.app.Application;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.net.NetManager;

public class SocketTagRemoveConfigurer implements OperationConfigurer {

	private final NetManager server;
	
	private Function<Data,String> idFn = StringWithVars.compile(":id");
	private List<Function<Data,String>> tagFns = new ArrayList<>();
	
	public SocketTagRemoveConfigurer(NetManager server) {
		this.server = server;
	}
	
	@Conf.At("id")
	public void id(String val) {
		idFn = StringWithVars.compile(val);
	}
	
	@Conf.Each("tag")
	public void tag(String val) {
		tagFns.add(StringWithVars.compile(val));
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("tag/remove", () -> new SocketTagRemoveOperation(server, ops.ctx().get(Application.IDENTITY), idFn, tagFns));
	}
	
}