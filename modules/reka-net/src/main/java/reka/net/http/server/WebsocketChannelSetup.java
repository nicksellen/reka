package reka.net.http.server;

import static reka.util.Util.createEntry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reka.Identity;
import reka.net.ChannelAttrs;
import reka.net.NetServerManager.SocketFlows;
import reka.net.NetSettings.Type;
import reka.net.websockets.WebSocketServerProtocolHandshakeHandler;
import reka.net.websockets.WebsocketFlowHandler;

import com.google.common.base.Splitter;

@Sharable
public class WebsocketChannelSetup extends MessageToMessageDecoder<FullHttpRequest> implements ChannelSetup<SocketFlows> {

	private static final WebSocketServerProtocolHandshakeHandler handshaker = new WebSocketServerProtocolHandshakeHandler();
	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	
	private final ConcurrentMap<String,SocketFlows> flows = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,Identity> identities = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,List<Entry<ChannelHandlerContext,FullHttpRequest>>> paused = new ConcurrentHashMap<>();

	private final ChannelGroup channels;
	private final int port;
	private final boolean ssl;
	
	public WebsocketChannelSetup(ChannelGroup channels, int port, boolean ssl) {
		this.channels = channels;
		this.port = port;
		this.ssl = ssl;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest req, List<Object> out) throws Exception {
		
		String host = hostSplitter.split(HttpHeaders.getHost(req, "localhost")).iterator().next();
		
		if (!flows.containsKey(host)) {
			ctx.close();
			return;
		}
		
		if (paused.containsKey(host)) {
			paused.get(host).add(createEntry(ctx, req.retain()));
			return;
		}
		
		setup(ctx, host, req.retain());
	}
	
	private void setup(ChannelHandlerContext ctx, String host, FullHttpRequest req) {

		SocketFlows flow = flows.get(host);
		
		if (flow == null) {
			// it's gone!
			req.release();
			ctx.disconnect();
			return;
		}
		
		Channel channel = ctx.channel();
		
		ctx.pipeline()
			.addLast(handshaker)
			.addLast(StringWebsocketEncoder.INSTANCE)
			.addLast(new WebsocketFlowHandler(flows.get(host)))
			.remove(this);
		
		channel.attr(ChannelAttrs.identity).set(identities.get(host));
		channel.attr(ChannelAttrs.host).set(host);
		channel.attr(ChannelAttrs.port).set(port);
		channel.attr(ChannelAttrs.type).set(Type.WEBSOCKET);
		
		channels.add(channel);
		
		ctx.fireChannelRead(req);
		
	}

	@Override
	public Runnable add(String host, Identity identity, SocketFlows flows) {
		this.flows.put(host, flows);
		this.identities.put(host, identity);
		return () -> remove(host);
	}

	private boolean remove(String host) {
		identities.remove(host);
		return flows.remove(host) != null;
	}

	@Override
	public Runnable pause(String host) {
		paused.computeIfAbsent(host, unused -> new ArrayList<>());
		return () -> resume(host);
	}

	private void resume(String host) {
		List<Entry<ChannelHandlerContext, FullHttpRequest>> ctxs = paused.get(host);
		if (ctxs == null) return;
		ctxs.forEach(e -> {
			ChannelHandlerContext ctx = e.getKey();
			FullHttpRequest req = e.getValue();
			setup(ctx, host, req);
		});
	}

	@Override
	public boolean isEmpty() {
		return flows.isEmpty();
	}

}
