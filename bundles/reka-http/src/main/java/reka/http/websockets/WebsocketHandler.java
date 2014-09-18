package reka.http.websockets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.Subscriber;
import reka.core.data.memory.MutableMemoryData;
import reka.http.server.HttpServerManager.WebsocketTriggers;

@ChannelHandler.Sharable
public class WebsocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final AttributeKey<String> hostAttr = AttributeKey.valueOf("host");
	
	private static final AttributeKey<String> idAttr = AttributeKey.valueOf("id");
	
	private final ConcurrentMap<String,WebsocketHost> hosts = new ConcurrentHashMap<>();
	
	public static class WebsocketHost {
		public final ConcurrentMap<IdentityKey<Object>,Topic> topics = new ConcurrentHashMap<>(); 
		public final ConcurrentMap<String,Channel> channels = new ConcurrentHashMap<>();
		public final List<Flow> onConnect = new ArrayList<>();
		public final List<Flow> onDisconnect = new ArrayList<>();
		public final List<Flow> onMessage = new ArrayList<>();
		
		public Optional<Channel> channel(String id) {
			return Optional.ofNullable(channels.get(id));
		}
		
		public Optional<Topic> topic(IdentityKey<Object> key) {
			return Optional.ofNullable(topics.get(key));
		}
	}
	
	public static class Topic {
		
		private final Map<Channel,Void> channels = Collections.synchronizedMap(new WeakHashMap<>());
		
		public Collection<Channel> channels() {
			return channels.keySet();
		}

		public void register(Channel channel) {
			channels.put(channel, null);
		}
		
	}

	public static interface WebsocketListener {
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
	
	public void add(String host, WebsocketTriggers d) {
		log.debug("adding ws host [{}]", host);
		WebsocketHost h = hosts.computeIfAbsent(host, (val) -> new WebsocketHost());
		
		// we're keeping topics if they match by name
		// this ensures the topics registration connections persist across deploys
		
		Map<IdentityKey<Object>, Topic> newtopics = new HashMap<>();
		
		d.topicKeys().forEach(key -> {
			Topic topic = null;
			for (Entry<IdentityKey<Object>, Topic> e : h.topics.entrySet()) {
				if (key.name().equals(e.getKey().name())) {
					topic = e.getValue();
				}
			}
			if (topic == null) topic = new Topic();
			newtopics.put(key, topic);
		});

		h.topics.clear();
		h.topics.putAll(newtopics);
		
		
		h.onConnect.clear();
		h.onConnect.addAll(d.onConnect());
		
		h.onDisconnect.clear();
		h.onDisconnect.addAll(d.onDisconnect());
		
		h.onMessage.clear();
		h.onMessage.addAll(d.onMessage());
		
	}

	public boolean remove(String host) {
		log.debug("removing ws host [{}]", host);
		WebsocketHost h = hosts.remove(host);
		if (h != null) {
			h.channels.forEach((id, ch) -> ch.disconnect());
			return true;
		} else {
			return false;
		}
	}
	
	public void forHost(String host, Consumer<WebsocketHost> c) {
		WebsocketHost h = hosts.get(host);
		if (h != null) c.accept(h);
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
			String host = ctx.attr(hostAttr).get();
			
			String id = UUID.randomUUID().toString();
			ctx.attr(idAttr).set(id);
			hosts.get(host).channels.put(id, ctx.channel());
			
			log.debug("{} connected to host {}!", id, host);

			for (Flow flow : hosts.get(host).onConnect) {
				
				MutableData data = MutableMemoryData.create();
				
				data.putString("host", host);
				data.putString("id", id);
				
				flow.run(ctx.executor(), data, resultData -> {
					resultData.getContent("reply").ifPresent(content -> {
						ctx.channel().writeAndFlush(new TextWebSocketFrame(content.asUTF8()));
					});
				});
				
			}
			
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		String id = ctx.attr(idAttr).get();
		String host = ctx.attr(hostAttr).get();
		log.debug("{} disconnected", id);
		WebsocketHost wshost = hosts.get(host);
		if (wshost != null) {
			wshost.channels.remove(id);
			for (Flow flow : wshost.onDisconnect) {
				flow.run(ctx.executor(), 
						 MutableMemoryData.create()
						 	.putString("host", host)
						 	.putString("id", id),
						 Subscriber.DO_NOTHING);
			}

		}
		
	}

	@Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
		String id = ctx.attr(idAttr).get();
		String host = ctx.attr(hostAttr).get();
		
		for (Flow flow : hosts.get(host).onMessage) {
		
			MutableData data = MutableMemoryData.create();
			
			data.putString("host", host);
			data.putString("id", id);
			data.putString("message", frame.text());
			
			flow.run(ctx.executor(), data, resultData -> {
				resultData.getContent("response").ifPresent(content -> {
					ctx.channel().writeAndFlush(new TextWebSocketFrame(content.asUTF8()));
				});
			});
		
		}
		
    }
    
}