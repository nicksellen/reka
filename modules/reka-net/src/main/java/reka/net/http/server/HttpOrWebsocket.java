package reka.net.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reka.Identity;
import reka.net.ChannelAttrs;
import reka.net.NetServerManager.HttpFlows;
import reka.net.NetServerManager.SocketFlows;
import reka.net.NetSettings.Type;
import reka.net.websockets.WebSocketServerProtocolHandshakeHandler;
import reka.net.websockets.WebsocketFlowHandler;

import com.google.common.base.Splitter;

@Sharable
public class HttpOrWebsocket extends MessageToMessageDecoder<FullHttpRequest> {
	
	private static final ChannelHandler DATASET_DECODER = new FullHttpToDatasetDecoder();
	private static final WebSocketServerProtocolHandshakeHandler handshaker = new WebSocketServerProtocolHandshakeHandler();

	private final int port;
	private final boolean ssl;
	
	private final ChannelGroup channels;
	
	private final ConcurrentMap<String,HttpFlows> httpFlows = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,Identity> httpIdentities = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,SocketFlows> websocketFlows = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,Identity> websocketIdentities = new ConcurrentHashMap<>();
	
	private static final String WS_HEADER = "Upgrade";
	private static final String WS_HEADER_VALUE = "websocket";
	
	public HttpOrWebsocket(ChannelGroup channels, int port, boolean ssl) {
		this.channels = channels;
		this.port = port;
		this.ssl = ssl;
	}

	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	
	public boolean isEmpty() {
		return httpFlows.isEmpty() && websocketFlows.isEmpty();
	}
	
	public void addHttp(Identity identity, String host, HttpFlows flows) {
		httpFlows.put(host, flows);
		httpIdentities.put(host, identity);
	}
	
	public boolean removeHttp(String host) {
		httpIdentities.remove(host);
		return httpFlows.remove(host) != null;
	}

	public void addWebsocket(Identity identity, String host, SocketFlows flows) {
		websocketFlows.put(host, flows);
		websocketIdentities.put(host, identity);
	}
	
	public boolean removeWebsocket(String host) {
		websocketFlows.remove(host);
		return websocketFlows.remove(host) != null; 
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest req, List<Object> out) throws Exception {

		Channel channel = ctx.channel();
		
		String host = hostSplitter.split(HttpHeaders.getHost(req, "localhost")).iterator().next();
		
		if (WS_HEADER_VALUE.equals(req.headers().get(WS_HEADER))) {
			
			if (!websocketFlows.containsKey(host)) {
				ctx.close();
				return;
			}
			
			ctx.pipeline()
				.addLast(handshaker)
				.addLast(StringWebsocketEncoder.INSTANCE)
				.addLast(new WebsocketFlowHandler(websocketFlows.get(host)))
				.remove(this);
			
			channel.attr(ChannelAttrs.identity).set(websocketIdentities.get(host));
			channel.attr(ChannelAttrs.host).set(host);
			channel.attr(ChannelAttrs.port).set(port);
			channel.attr(ChannelAttrs.type).set(Type.WEBSOCKET);
			
			channels.add(channel);
			
		} else {
			
			if (!httpFlows.containsKey(host)) {
				ctx.close();
				return;
			}
			
			ctx.pipeline()
				.addLast(DATASET_DECODER)
				.addLast(ssl ? DataToHttpEncoder.SSL : DataToHttpEncoder.NORMAL)
				.addLast(new HttpFlowHandler(httpFlows.get(host), ctx.channel()))
				.remove(this);
			
			channel.attr(ChannelAttrs.identity).set(httpIdentities.get(host));
			channel.attr(ChannelAttrs.host).set(host);
			channel.attr(ChannelAttrs.port).set(port);
			channel.attr(ChannelAttrs.type).set(Type.HTTP);
			
			channels.add(channel);
			
		}

		// push the request back in...
		out.add(req.retain());
	}

}
