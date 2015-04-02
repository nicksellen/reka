package reka.net;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;

public class SocketInitializer extends ChannelInitializer<SocketChannel> {

	private final ChannelHandler handler;
	
	public SocketInitializer(ChannelHandler handler) {
		this.handler = handler;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline()
			.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
			.addLast(new StringDecoderIgnoringNewlines())
			.addLast(new StringEncoderWithNewline())
			.addLast(handler);
	}
	
}
