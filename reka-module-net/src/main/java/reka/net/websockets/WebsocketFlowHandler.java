package reka.net.websockets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.util.HashSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.Data;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.net.ChannelAttrs;
import reka.net.NetManager.SocketFlows;

public class WebsocketFlowHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	private static final Logger log = LoggerFactory.getLogger(WebsocketFlowHandler.class);
	
	private final SocketFlows flows;
	
	public WebsocketFlowHandler(SocketFlows flows) {
		this.flows = flows;
	}

	public static interface SocketListener {
		void connect();
		void disconnect();
		void message(String msg);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {

			Channel channel = ctx.channel();
			
			String host = channel.attr(ChannelAttrs.host).get();
			
			String id = UUID.randomUUID().toString();
			channel.attr(ChannelAttrs.id).set(id);
			channel.attr(ChannelAttrs.tags).set(new HashSet<String>());

			log.debug("{} connected to host {}!", id, host);
			
			trigger(flows.onConnect(), MutableMemoryData.create()
				.putString("host", host)
				.putString("id", id), ctx);

		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		Channel channel = ctx.channel();
		String host = channel.attr(ChannelAttrs.host).get();
		String id = channel.attr(ChannelAttrs.id).get();
		log.debug("{} disconnected", id);
		trigger(flows.onDisconnect(), MutableMemoryData.create()
				.putString("host", host)
				.putString("id", id), ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
		Channel channel = ctx.channel();
		String id = channel.attr(ChannelAttrs.id).get();
		String host = channel.attr(ChannelAttrs.host).get();
		trigger(flows.onMessage(), MutableMemoryData.create()
				.putString("host", host)
				.putString("id", id)
				.putString("message", frame.text()), ctx);
	}
	
    private void trigger(Flow flow, Data data, ChannelHandlerContext ctx) {
		flow.prepare().mutableData(MutableMemoryData.from(data)).complete(resultData -> {
			resultData.getContent("reply").ifPresent(content -> {
				ctx.channel().writeAndFlush(content.asUTF8());
			});
		}).run();
    }

}