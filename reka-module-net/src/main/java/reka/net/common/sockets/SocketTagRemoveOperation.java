package reka.net.common.sockets;

import java.util.List;
import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.identity.Identity;
import reka.net.ChannelAttrs;
import reka.net.NetManager;

public class SocketTagRemoveOperation implements Operation {

	private final NetManager server;
	private final Function<Data,String> idFn;
	private final List<Function<Data,String>> tagFns;
	private final Identity identity;
	
	public SocketTagRemoveOperation(NetManager server, Identity identity, Function<Data,String> idFn, List<Function<Data,String>> tagFns) {
		this.server = server;
		this.idFn = idFn;
		this.tagFns = tagFns;
		this.identity = identity;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		server.channels(identity).withAttr(ChannelAttrs.id, idFn.apply(data)).forEach(channel -> {
			tagFns.forEach(tagFn -> {
				channel.attr(ChannelAttrs.tags).get().remove(tagFn.apply(data));
			});
		});
	}
	
}