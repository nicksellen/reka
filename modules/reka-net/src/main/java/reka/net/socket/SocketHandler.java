package reka.net.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.core.data.memory.MutableMemoryData;
import reka.net.ChannelAttrs;
import reka.net.ChannelAttrs.ChannelIdMatcher;
import reka.net.NetServerManager.SocketTriggers;

@Sharable
public class SocketHandler extends SimpleChannelInboundHandler<String> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public final List<Flow> onConnect = new ArrayList<>();
	public final List<Flow> onDisconnect = new ArrayList<>();
	public final List<Flow> onMessage = new ArrayList<>();
	
	private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	public void setTriggers(SocketTriggers triggers) {
		reset(onConnect, triggers.onConnect());
		reset(onDisconnect, triggers.onDisconnect());
		reset(onMessage, triggers.onMessage());
	}
	
	public Optional<Channel> channel(String id) {
		return channels.stream().filter(new ChannelIdMatcher(id)).findFirst();
	}
	
	public ChannelGroup channels() {
		return channels;
	}
	
	private void reset(List<Flow> list, List<Flow> incoming) {
		list.clear();
		list.addAll(incoming);
	}
	
    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
    	String id = UUID.randomUUID().toString();
    	Channel channel = ctx.channel();
		channel.attr(ChannelAttrs.id).set(id);
		channel.attr(ChannelAttrs.tags).set(new HashSet<String>());
		channels.add(ctx.channel());
		trigger(onConnect, MutableMemoryData.create().putString("id", id), ctx);
    }
    
    @Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		String id = ctx.channel().attr(ChannelAttrs.id).get();
		log.debug("{} disconnected", id);
		channels.remove(id);
		trigger(onDisconnect, MutableMemoryData.create().putString("id", id), ctx);
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		trigger(onMessage, MutableMemoryData.create()
				.putString("id", ctx.channel().attr(ChannelAttrs.id).get())
				.putString("line", msg), ctx);
	}
	
    private void trigger(List<Flow> flows, Data data, ChannelHandlerContext ctx) {
    	for (Flow flow : flows) {
    		flow.prepare().data(MutableMemoryData.from(data)).complete(resultData -> {
				resultData.getContent("reply").ifPresent(content -> {
					ctx.channel().writeAndFlush(content.asUTF8());
				});
    		}).run();
    	}
    }

}
