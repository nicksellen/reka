package reka.net.http.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.List;

@Sharable
public class HttpOrWebsocket extends MessageToMessageDecoder<FullHttpRequest> {
	
	private static final String WS_HEADER = "Upgrade";
	private static final String WS_HEADER_VALUE = "websocket";
	
	private final HttpChannelSetup http;
	private final WebsocketChannelSetup websocket;
	
	public HttpOrWebsocket(HttpChannelSetup http, WebsocketChannelSetup websocket) { 
		this.http = http;
		this.websocket = websocket;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest req, List<Object> out) throws Exception {
		if (WS_HEADER_VALUE.equals(req.headers().get(WS_HEADER))) {
			ctx.pipeline().addLast(websocket);
		} else {
			ctx.pipeline().addLast(http);
		}
		out.add(req.retain());
	}

}
