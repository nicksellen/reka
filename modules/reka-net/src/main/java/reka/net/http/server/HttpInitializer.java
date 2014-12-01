package reka.net.http.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpInitializer extends ChannelInitializer<SocketChannel> {

	private final ChannelHandler handler;
	
	public HttpInitializer(ChannelHandler handler) {
		this.handler = handler;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
				new HttpRequestDecoder(),
				new HttpObjectAggregator(1024 * 1024 * 500), // 500mb
				//new HttpContentCompressor(),
				new HttpResponseEncoder(),
				new ChunkedWriteHandler(), 
				handler);	
	}
	
}
