package reka.net.http.server;

import static reka.util.Util.unchecked;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLException;

import reka.net.NetSettings.TlsSettings;

public class HttpInitializer extends ChannelInitializer<SocketChannel> {

	private final ChannelHandler handler;
	private final SslContext ssl;
	
	public HttpInitializer(ChannelHandler handler, TlsSettings sslSettings) {
		this.handler = handler;
		if (sslSettings != null) {
			try {
				this.ssl = SslContext.newServerContext(SslProvider.OPENSSL, sslSettings.certChainFile(), sslSettings.keyFile());
			} catch (SSLException e) {
				throw unchecked(e);
			}
		} else {
			this.ssl = null;
		}
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.config().setAutoRead(true);
		ChannelPipeline pipeline = ch.pipeline();
		if (ssl != null) {
			pipeline.addLast("ssl", ssl.newHandler(ch.alloc()));
		}
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(1024 * 1024 * 500)); // 500mb
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("chunking", new ChunkedWriteHandler()); 
		pipeline.addLast("handler", handler);	
	}
	
}
