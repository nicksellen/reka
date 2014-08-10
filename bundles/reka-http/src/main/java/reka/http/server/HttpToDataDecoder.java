package reka.http.server;

import static reka.api.Path.dots;
import static reka.api.content.Contents.trueValue;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map.Entry;

import reka.api.Path.PathElements;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

@Sharable
public class HttpToDataDecoder extends MessageToMessageDecoder<HttpRequest> {
	
	@Override
	protected void decode(ChannelHandlerContext ctx, HttpRequest request, List<Object> out) throws Exception {
		
		if (request.getUri().equals("/favicon.ico")) {
			return;
		}
		
		final MutableData data = MutableMemoryData.create();

		MutableData params = data.createMapAt(Request.PARAMS);
		MutableData headers = data.createMapAt(Request.HEADERS);
		
		QueryStringDecoder qs = new QueryStringDecoder(request.getUri());
		
		String host = HttpHeaders.getHost(request).split(":")[0];

		/*
		InetSocketAddress local = (InetSocketAddress) ctx.channel().localAddress();
		int port = local.getPort();
		data.putString("something", format("%s://%s%s", "http", host, port == 80 ? "" : ":" + port));
		*/
		
		data.putString(Request.PATH, QueryStringDecoder.decodeComponent(qs.path()))
			.putString(Request.HOST, host);
		
		String httpMethod = request.getMethod().toString(); // may be overridden by param later
		
		// params

		for (Entry<String, List<String>> entry : qs.parameters().entrySet()) {
			for (String value : entry.getValue()) {
				params.putString(dots(entry.getKey()).add(PathElements.nextIndex()), value);
			}
		}

		// request headers

		for (Entry<String, String> header : request.headers()) {
			headers.putString(header.getKey(), header.getValue());
		}
		
		if ("HEAD".equals(httpMethod)) {
			httpMethod = "GET";
			data.put(Response.HEAD, trueValue());
		}
		
		data.putString(Request.METHOD, httpMethod);
		
		out.add(data);
	}

}
