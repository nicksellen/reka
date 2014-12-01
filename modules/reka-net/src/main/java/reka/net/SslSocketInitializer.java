package reka.net;

import static reka.util.Util.unchecked;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;

import java.io.File;

import javax.net.ssl.SSLException;

public class SslSocketInitializer extends ChannelInitializer<SocketChannel> {

	private final ChannelHandler handler;
	private final SslContext ssl;
	
	public SslSocketInitializer(ChannelHandler handler, File certChainFile, File keyFile) {
		this.handler = handler;
		try {
			this.ssl = SslContext.newServerContext(SslProvider.OPENSSL, certChainFile, keyFile);
		} catch (SSLException e) {
			throw unchecked(e);
		}
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline()
			.addLast(ssl.newHandler(ch.alloc()))
			.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
			.addLast(new StringDecoder())
			.addLast(new StringEncoder())
			.addLast(handler);
	}
	
}
