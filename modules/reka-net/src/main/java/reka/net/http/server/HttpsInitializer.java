package reka.net.http.server;

import static reka.util.Util.unchecked;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;

import javax.net.ssl.SSLException;

public class HttpsInitializer extends ChannelInitializer<SocketChannel> {

	private final ChannelHandler handler;
	private final SslContext ssl;
	
	public HttpsInitializer(ChannelHandler handler, File certChainFile, File keyFile) {
		this.handler = handler;
		try {
			this.ssl = SslContext.newServerContext(SslProvider.OPENSSL, certChainFile, keyFile);
		} catch (SSLException e) {
			throw unchecked(e);
		}
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
				ssl.newHandler(ch.alloc()),
				new HttpRequestDecoder(),
				new HttpObjectAggregator(1024 * 1024 * 500), // 500mb
				//new HttpContentCompressor(),
				new HttpResponseEncoder(),
				new ChunkedWriteHandler(), 
				handler);
	}
	
}
