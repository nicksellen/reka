package reka.net.http.server;

import static reka.data.content.Contents.utf8;
import static reka.util.Path.COOKIES;
import static reka.util.Path.HEADERS;
import static reka.util.Path.PARAMS;
import static reka.util.Path.REQUEST;
import static reka.util.Path.dots;
import static reka.util.Path.path;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.MutableData;
import reka.data.content.types.BooleanContent;
import reka.data.memory.MutableMemoryData;
import reka.net.http.converters.DefaultMessageToDataConverter;
import reka.net.http.converters.FormUrlEncodedMessageToDataConverter;
import reka.net.http.converters.HttpMessageToDataConverter;
import reka.net.http.converters.JsonMessageToDataConverter;
import reka.net.http.converters.MultipartRequestMessageToDataConverter;
import reka.net.http.converters.PlainTextMessageToDataConverter;
import reka.util.Path.PathElements;
import reka.util.Path.Request;
import reka.util.Path.Response;

import com.google.common.base.Splitter;

@Sharable
public class HttpRequestToDataDecoder extends MessageToMessageDecoder<FullHttpRequest> {
	
	static final Logger log = LoggerFactory.getLogger(HttpRequestToDataDecoder.class);

	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	private static final Splitter semicolonSplitter = Splitter.on(";").limit(2);
	
	private static final Map<String,HttpMessageToDataConverter> contentHandlers = new HashMap<>();
	private static final HttpMessageToDataConverter multipartHandler = new MultipartRequestMessageToDataConverter();
	private static final HttpMessageToDataConverter defaultContentHandler = new DefaultMessageToDataConverter();
	
	static {
		contentHandlers.put("application/json", new JsonMessageToDataConverter());
		contentHandlers.put("application/x-www-form-urlencoded", new FormUrlEncodedMessageToDataConverter());
		contentHandlers.put("text/plain", new PlainTextMessageToDataConverter());
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest req, List<Object> out) throws Exception {
		
		final MutableData data = MutableMemoryData.create();

		if (!HttpHeaders.isKeepAlive(req)) {
			data.putBool(HttpHostHandler.CLOSE_CHANNEL, true);
		}

		MutableData requestData = data.createMapAt(REQUEST);
		MutableData params = requestData.createMapAt(PARAMS);
		MutableData headers = requestData.createMapAt(HEADERS);
		MutableData cookies = requestData.createMapAt(COOKIES);
		
		String host = hostSplitter.split(HttpHeaders.getHost(req, "")).iterator().next();

		QueryStringDecoder qs = new QueryStringDecoder(req.getUri());
		
		data.putString(Request.PATH, QueryStringDecoder.decodeComponent(qs.path()))
			.putString(Request.PATH_BASE, "")
			.putString(Request.HOST, host);

		String httpMethod = req.getMethod().toString();
		if ("HEAD".equals(httpMethod)) {
			httpMethod = "GET";
			data.put(Response.HEAD, BooleanContent.TRUE);
		}
		data.putString(Request.METHOD, httpMethod);
		
		// params

		for (Entry<String, List<String>> entry : qs.parameters().entrySet()) {
			for (String value : entry.getValue()) {
				params.putOrAppend(dots(entry.getKey()), utf8(value));
			}
		}

		// headers

		for (Entry<String, String> header : req.headers()) {
			headers.put(path(PathElements.name(header.getKey())), utf8(header.getValue()));
		}
		
		// cookies mmmm
		
		String cookieHeader = req.headers().get(com.google.common.net.HttpHeaders.COOKIE);
		if (cookieHeader != null) {
			for (Cookie cookie : CookieDecoder.decode(cookieHeader)) {
				cookies.putMap(cookie.getName(), c -> {
					c.putString("value", cookie.getValue());
					if (cookie.getDomain() != null) c.putString("domain", cookie.getDomain());
					if (cookie.getPath() != null) c.putString("path", cookie.getPath());
					if (cookie.getMaxAge() != Long.MIN_VALUE) c.putLong("max-age", cookie.getMaxAge());	
				});
			}
		}
		
		// body
		
		if (req.getMethod().equals(HttpMethod.POST) || req.getMethod().equals(HttpMethod.PUT)) {
			
			String contentType = req.headers().get(HttpHeaders.Names.CONTENT_TYPE);
			if (contentType == null) contentType = "application/octet-stream";
			
			// just take the first bit if there is a semicolon, ignore the rest (charset, etc)
			contentType = semicolonSplitter.split(contentType).iterator().next().toLowerCase();
			
			data.putString(Request.Headers.CONTENT_TYPE, contentType);
			
			if (contentType.startsWith("multipart/")) {
				multipartHandler.processData(req, data, contentType);
			} else {
				contentHandlers.getOrDefault(contentType, defaultContentHandler).processData(req, requestData, contentType);
			}
		}
		
		out.add(data);
	}

}
