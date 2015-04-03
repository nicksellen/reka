package reka.net.http.operations;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

import reka.data.MutableData;
import reka.flow.ops.AsyncOperation;
import reka.flow.ops.OperationContext;
import reka.net.http.server.HttpResponseToDataDecoder;
import reka.util.Path;

public class HttpRequestOperation implements AsyncOperation {

	private final Bootstrap bootstrap;

	private final String host;
	private final int port;
	private final String path;
	private final Path into;

	public HttpRequestOperation(EventLoopGroup group, Class<? extends Channel> channelType, String url, Path into) {
		URI uri = makeURI(url);
		this.port = uri.getPort();
		this.host = uri.getHost();
		if (uri.getRawQuery() != null) {
			this.path = uri.getRawPath() + "?" + uri.getRawQuery();
		} else {
			this.path = uri.getRawPath();
		}
		this.into = into;
		final ChannelHandler decoder = new HttpResponseToDataDecoder();
		
		bootstrap = new Bootstrap()
				.group(group)
				.channel(channelType)
				.handler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("http", new HttpClientCodec());
						pipeline.addLast("aggregator", new HttpObjectAggregator(5242880));
						pipeline.addLast("decode", decoder);
					}
					
				});
	}

	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {

		bootstrap.connect(host, port).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture cf) throws Exception {
				Channel ch = cf.channel();
				ch.pipeline().addLast(new SimpleChannelInboundHandler<MutableData>(){

					@Override
					protected void channelRead0(ChannelHandlerContext ctx, MutableData msg) throws Exception {
						data.put(into, msg);
						res.done();
					}
					
				});

				HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
				request.headers().set(HttpHeaders.Names.HOST, host);
				request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);

				ch.writeAndFlush(request);
			}
		});
	}

	private URI makeURI(String url) {
		try {
			URIBuilder uri = new URIBuilder(url);
			
			if ("".equals(uri.getPath())) {
				uri.setPath("/");
			}
			
			if (uri.getPort() == -1) {
				switch (uri.getScheme()) {
					case "http": uri.setPort(80); break;
					case "https": uri.setPort(443); break;
					default:
						throw runtime("don't know which port to pick for %s", uri.getScheme());
				} 
			}
			
			return uri.build();
		} catch (URISyntaxException e) {
			throw unchecked(e);
		}
	}

}
