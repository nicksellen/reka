package reka.http.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.List;

import reka.http.websockets.WebSocketServerProtocolHandshakeHandler;
import reka.http.websockets.WebsocketHandler;

import com.google.common.base.Splitter;

@Sharable
public class HttpOrWebsocket extends MessageToMessageDecoder<FullHttpRequest> {
	
	private static final ChannelHandler DATASET_DECODER = new FullHttpToDatasetDecoder();
	private static final WebSocketServerProtocolHandshakeHandler handshaker = new WebSocketServerProtocolHandshakeHandler();

	private final ChannelHandler http;
	private final WebsocketHandler websockets;
	private final boolean ssl;
	
	private static final String WS_HEADER = "Upgrade";
	private static final String WS_HEADER_VALUE = "websocket";
	
	public HttpOrWebsocket(ChannelHandler http, WebsocketHandler websockets, boolean ssl) {
		this.http = http;
		this.websockets = websockets;
		this.ssl = ssl;
	}

	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest req, List<Object> out) throws Exception {
		
		if (WS_HEADER_VALUE.equals(req.headers().get(WS_HEADER))) {
			
			String host = hostSplitter.split(HttpHeaders.getHost(req, "localhost")).iterator().next();
			
			if (!websockets.hostExists(host)) {
				ctx.close();
				return;
			}
			
			ctx.pipeline()
				.addLast(handshaker, websockets)
				.remove(this);
			
			ctx.pipeline().context(websockets).attr(WebsocketHandler.hostAttr).set(host);
			
		} else {
			
			ctx.pipeline()
				.addLast(DATASET_DECODER, new DataToHttpEncoder(ssl), http)
				.remove(this);
			
		}

		// push the request back in...
		out.add(req.retain());
	}

}
