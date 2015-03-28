package reka.net.common.sockets;

import java.util.List;
import java.util.function.Function;

import reka.Identity;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.ChannelAttrs;
import reka.net.NetManager;

public class SocketTagAddOperation implements Operation {

	private final NetManager server;
	private final Function<Data,String> idFn;
	private final List<Function<Data,String>> tagFns;
	private final Identity identity;
	
	public SocketTagAddOperation(NetManager server, Identity identity, Function<Data,String> idFn, List<Function<Data,String>> tagFns) {
		this.server = server;
		this.idFn = idFn;
		this.tagFns = tagFns;
		this.identity = identity;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channels(identity).withAttr(ChannelAttrs.id, idFn.apply(data)).forEach(channel -> {
			tagFns.forEach(tagFn -> {
				channel.attr(ChannelAttrs.tags).get().add(tagFn.apply(data));
			});
		});
	}
	
}