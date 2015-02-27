package reka.net.common.sockets;

import java.util.List;
import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.ChannelAttrs;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketTagRemoveOperation implements Operation {

	private final NetServerManager server;
	private final Function<Data,String> idFn;
	private final List<Function<Data,String>> tagFns;
	private final NetSettings settings;
	
	public SocketTagRemoveOperation(NetServerManager server, NetSettings settings, Function<Data,String> idFn, List<Function<Data,String>> tagFns) {
		this.server = server;
		this.idFn = idFn;
		this.tagFns = tagFns;
		this.settings = settings;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channel(settings, idFn.apply(data), channel -> {
			tagFns.forEach(tagFn -> {
				channel.attr(ChannelAttrs.tags).get().remove(tagFn.apply(data));
			});
		});
	}
	
}