package reka.net.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.Data;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.identity.IdentityStore;
import reka.identity.ImmutableIdentityStore.Builder;
import reka.net.ChannelAttrs;
import reka.net.NetManager.SocketFlows;
import reka.net.NetModule;

public class SocketFlowHandler extends SimpleChannelInboundHandler<String> {
	
	private static final Logger log = LoggerFactory.getLogger(SocketFlowHandler.class);
	
	private volatile SocketFlows flows = SocketFlows.NO_FLOWS;
	
	public void setFlows(SocketFlows flows) {
		this.flows = flows;
	}
	
	public boolean unsetFlows(SocketFlows flows) {
		if (!this.flows.equals(flows)) return false;
		flows = SocketFlows.NO_FLOWS;
		return true;
	}
	 
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    	super.channelActive(ctx);
    	String id = UUID.randomUUID().toString();
    	Channel channel = ctx.channel();
		channel.attr(ChannelAttrs.id).set(id);
		channel.attr(ChannelAttrs.tags).set(new HashSet<String>());
		trigger(flows.onConnect(), MutableMemoryData.create().putString("id", id), ctx);
    }
    
    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		String id = ctx.channel().attr(ChannelAttrs.id).get();
		log.debug("{} disconnected", id);
		trigger(flows.onDisconnect(), MutableMemoryData.create().putString("id", id), ctx);
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		trigger(flows.onMessage(), MutableMemoryData.create()
				.putString("id", ctx.channel().attr(ChannelAttrs.id).get())
				.putString("line", msg), ctx);
	}
	
    private void trigger(Flow flow, Data data, ChannelHandlerContext ctx) {
		Builder store = IdentityStore.immutableBuilder();
		store.put(NetModule.Keys.channel, ctx.channel());
		flow.prepare().store(store.build()).mutableData(MutableMemoryData.from(data)).complete(resultData -> {
			resultData.getContent("reply").ifPresent(content -> {
				ctx.channel().writeAndFlush(content.asUTF8());
			});
		}).run();
    }

}
