package reka.net.websockets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.core.data.memory.MutableMemoryData;
import reka.net.ChannelAttrs;
import reka.net.ChannelAttrs.ChannelHostMatcher;
import reka.net.ChannelAttrs.ChannelIdMatcher;
import reka.net.NetServerManager.SocketTriggers;

@ChannelHandler.Sharable
public class WebsocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<String, SocketHost> hosts = new ConcurrentHashMap<>();

	public static class SocketHost {

		public final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		public final List<Flow> onConnect = new ArrayList<>();
		public final List<Flow> onDisconnect = new ArrayList<>();
		public final List<Flow> onMessage = new ArrayList<>();

		public Optional<Channel> channel(String id) {
			return channels.stream().filter(Channel::isActive).filter(new ChannelIdMatcher(id)).findFirst();
		}

	}

	public static class Topic {

		private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		public ChannelGroup channels() {
			return channels;
		}

		public void register(Channel channel) {
			channels.add(channel);
		}

	}

	public static interface SocketListener {
		void connect();

		void disconnect();

		void message(String msg);
	}

	public boolean hostExists(String host) {
		return hosts.containsKey(host);
	}

	public boolean isEmpty() {
		return hosts.isEmpty();
	}

	public void add(String host, SocketTriggers triggers) {

		log.debug("adding ws host [{}]", host);

		SocketHost ws = hosts.computeIfAbsent(host, (val) -> new SocketHost());

		ws.onConnect.clear();
		ws.onConnect.addAll(triggers.onConnect());

		ws.onDisconnect.clear();
		ws.onDisconnect.addAll(triggers.onDisconnect());

		ws.onMessage.clear();
		ws.onMessage.addAll(triggers.onMessage());

	}

	public boolean remove(String host) {
		log.debug("removing ws host [{}]", host);
		SocketHost h = hosts.remove(host);
		if (h != null) {
			h.channels.disconnect(new ChannelHostMatcher(host));
			return true;
		} else {
			return false;
		}
	}

	public void forHost(String host, Consumer<SocketHost> c) {
		SocketHost h = hosts.get(host);
		if (h != null) c.accept(h);
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
			
			forHost(host, ws -> {
				ws.channels.add(ctx.channel());
				trigger(ws.onConnect, MutableMemoryData.create()
					.putString("host", host)
					.putString("id", id), ctx);
			});

		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		Channel channel = ctx.channel();
		String id = channel.attr(ChannelAttrs.id).get();
		String host = channel.attr(ChannelAttrs.host).get();
		log.debug("{} disconnected", id);
		forHost(host, ws -> {
			trigger(ws.onDisconnect, MutableMemoryData.create()
					.putString("host", host)
					.putString("id", id), ctx);
		});
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
		Channel channel = ctx.channel();
		String id = channel.attr(ChannelAttrs.id).get();
		String host = channel.attr(ChannelAttrs.host).get();
		forHost(host, ws -> {
			trigger(ws.onMessage, MutableMemoryData.create()
					.putString("host", host)
					.putString("id", id)
					.putString("message", frame.text()), ctx);
		});
	}
	
    private void trigger(List<Flow> flows, Data data, ChannelHandlerContext ctx) {
    	for (Flow flow : flows) {
    		flow.prepare().mutableData(MutableMemoryData.from(data)).complete(resultData -> {
				resultData.getContent("reply").ifPresent(content -> {
					ctx.channel().writeAndFlush(content.asUTF8());
				});
    		}).run();
    	}
    }

}