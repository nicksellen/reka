package reka.net.http.server;

import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.utf8;
import static reka.util.Path.HEADERS;
import static reka.util.Path.STATUS;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.net.http.converters.DefaultMessageToDataConverter;
import reka.net.http.converters.HttpMessageToDataConverter;
import reka.net.http.converters.JsonMessageToDataConverter;
import reka.net.http.converters.MultipartRequestMessageToDataConverter;
import reka.net.http.converters.PlainTextMessageToDataConverter;
import reka.util.Path;
import reka.util.Path.Request;

import com.google.common.base.Splitter;

@Sharable
public class HttpResponseToDataDecoder extends MessageToMessageDecoder<FullHttpResponse> {
	
	private static final Map<String,HttpMessageToDataConverter> contentHandlers = new HashMap<>();
	private static final HttpMessageToDataConverter multipartHandler = new MultipartRequestMessageToDataConverter();
	private static final HttpMessageToDataConverter defaultContentHandler = new DefaultMessageToDataConverter();
	
	private static final Splitter semicolonSplitter = Splitter.on(";").limit(2);
	
	static {
		contentHandlers.put("application/json", new JsonMessageToDataConverter());
		contentHandlers.put("text/plain", new PlainTextMessageToDataConverter());
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpResponse res, List<Object> out) throws Exception {
		
		final MutableData data = MutableMemoryData.create();
		
		data.put(STATUS, integer(res.getStatus().code()));
		
		MutableData headers = data.createMapAt(HEADERS);

		// headers

		for (Entry<String, String> header : res.headers()) {
			headers.put(Path.path(header.getKey()), utf8(header.getValue()));
		}
		
		// body
			
		String contentType = res.headers().get(HttpHeaders.Names.CONTENT_TYPE);
		if (contentType == null) contentType = "application/octet-stream";
		
		// just take the first bit if there is a semicolon, ignore the rest (charset, etc)
		contentType = semicolonSplitter.split(contentType).iterator().next().toLowerCase();
		
		data.putString(Request.Headers.CONTENT_TYPE, contentType);
		
		if (contentType.startsWith("multipart/")) {
			multipartHandler.processData(res, data, contentType);
		} else {
			contentHandlers.getOrDefault(contentType, defaultContentHandler).processData(res, data, contentType);
		}
		
		out.add(data);
	}

}
