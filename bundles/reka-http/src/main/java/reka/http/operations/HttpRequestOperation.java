package reka.http.operations;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import org.apache.http.client.utils.URIBuilder;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.http.server.HttpResponseToDatasetDecoder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

public class HttpRequestOperation implements AsyncOperation {

	private final Bootstrap bootstrap;

	private final int port;
	private final URI uri;
	private final String host;
	private final Path out;

	public HttpRequestOperation(NioEventLoopGroup group, String url, Path out) {
		uri = makeURI(url);
		this.out = out;
		port = uri.getPort();
		host = uri.getHost();
		final ChannelHandler decoder = new HttpResponseToDatasetDecoder();
		bootstrap = new Bootstrap().group(group)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch)
							throws Exception {
						ch.pipeline().addLast(new HttpClientCodec(),
								new HttpObjectAggregator(5242880), decoder);
					}
				});
	}

	private static class ContentStoreHolder implements Callable<MutableData> {

		private final MutableData data;
		private final ListenableFutureTask<MutableData> task;

		ContentStoreHolder(MutableData data) {
			this.data = data;
			this.task = ListenableFutureTask.create(this);
		}

		public ListenableFuture<MutableData> future() {
			return task;
		}

		public void run() {
			task.run();
		}

		@Override
		public MutableData call() throws Exception {
			return data;
		}

	}

	private static class ResponseHandler extends
			SimpleChannelInboundHandler<MutableData> {

		private final ContentStoreHolder holder;
		private final Path out;

		ResponseHandler(ContentStoreHolder holder, Path out) {
			this.holder = holder;
			this.out = out;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext context, MutableData msg) throws Exception {
			//holder.data.createMapAt(path("rr")).merge(msg);
			holder.data.put(out, msg);
			holder.run();
		}

	}

	@Override
	public ListenableFuture<MutableData> call(MutableData data) {
		
		final ContentStoreHolder holder = new ContentStoreHolder(data);

		ChannelFuture channelFuture = bootstrap.connect(host, port);
		channelFuture.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture cf) throws Exception {
				Channel ch = cf.channel();
				ch.pipeline().addLast(new ResponseHandler(holder, out));

				HttpRequest request = new DefaultHttpRequest(
						HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
				request.headers().set(HttpHeaders.Names.HOST, host);
				request.headers().set(HttpHeaders.Names.CONNECTION,
						HttpHeaders.Values.CLOSE);
				// request.headers().put(HttpHeaders.Names.ACCEPT_ENCODING,
				// HttpHeaders.Values.GZIP);

				ch.writeAndFlush(request);//.addListener(new ResponseHandler(holder));
			}
		});

		return holder.future();
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
