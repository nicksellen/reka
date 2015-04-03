package reka.net.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import reka.api.IdentityStore;
import reka.api.IdentityStoreReader;
import reka.data.MutableData;
import reka.flow.Flow;
import reka.net.NetManager.HttpFlows;
import reka.net.NetModule;

public class HttpFlowHandler extends SimpleChannelInboundHandler<MutableData> {
	
	private final Flow flow;
	private final IdentityStoreReader store;
	
	public HttpFlowHandler(HttpFlows flows, Channel channel) {
		this.flow = flows.onMessage();
		this.store = IdentityStore.immutableBuilder().put(NetModule.Keys.channel, channel).build();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext context, MutableData data) {
		flow.run(context.executor(), context.executor(), data, new ChannelHandlerContextDataSubscriber(context), store, true);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}
	
}
