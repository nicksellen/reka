package reka.net.http.server;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.CharBuffer;
import java.util.List;

import com.google.common.base.Charsets;

@Sharable
final class StringWebsocketEncoder extends MessageToMessageEncoder<CharSequence> {
	
	public static final StringWebsocketEncoder INSTANCE = new StringWebsocketEncoder();

	@Override
	protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) throws Exception {
		if (msg.length() == 0) {
            return;
        }
		out.add(new TextWebSocketFrame(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(msg), Charsets.UTF_8)));
	}
	
}