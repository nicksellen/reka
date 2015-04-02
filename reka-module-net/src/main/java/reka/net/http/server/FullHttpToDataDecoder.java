package reka.net.http.server;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElements;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.content.types.BooleanContent;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.base.Splitter;

@Sharable
public class FullHttpToDataDecoder extends MessageToMessageDecoder<FullHttpRequest> {
	
	private static final Logger log = LoggerFactory.getLogger(FullHttpToDataDecoder.class);
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();
		
	private static final String FORM_FIELD_METHOD = "_method";

	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	private static final Splitter semicolonSplitter = Splitter.on(";").limit(2);
	
	private static final Map<String,RequestDataHandler> contentHandlers = new HashMap<>();
	private static final RequestDataHandler multipartHandler = new MultipartDataHandler();
	private static final RequestDataHandler defaultContentHandler = new DefaultDataHandler();
	
	static {
		contentHandlers.put("application/json", new JsonDataHandler());
		contentHandlers.put("application/x-www-form-urlencoded", new FormUrlEncodedDataHandler());
		contentHandlers.put("text/plain", new PlainTextDataHandler());
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) throws Exception {
		
		final MutableData data = MutableMemoryData.create();

		if (!HttpHeaders.isKeepAlive(request)) {
			data.putBool(HttpHostHandler.CLOSE_CHANNEL, true);
		}

		MutableData params = data.createMapAt(Request.PARAMS);
		MutableData headers = data.createMapAt(Request.HEADERS);
		MutableData cookies = data.createMapAt(Request.COOKIES);
		
		String host = hostSplitter.split(HttpHeaders.getHost(request, "")).iterator().next();

		QueryStringDecoder qs = new QueryStringDecoder(request.getUri());
		
		data.putString(Request.PATH, QueryStringDecoder.decodeComponent(qs.path()))
			.putString(Request.PATH_BASE, "")
			.putString(Request.HOST, host);

		String httpMethod = request.getMethod().toString();
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

		// request headers

		for (Entry<String, String> header : request.headers()) {
			headers.put(path(PathElements.name(header.getKey())), utf8(header.getValue()));
		}
		
		// cookies mmmm
		
		String cookieHeader = request.headers().get(com.google.common.net.HttpHeaders.COOKIE);
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
		
		// request data
		
		if (request.getMethod().equals(HttpMethod.POST) || request.getMethod().equals(HttpMethod.PUT)) {
			
			String contentType = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
			if (contentType == null) contentType = "application/octet-stream";
			
			// just take the first bit if there is a semicolon, ignore the rest (charset, etc)
			contentType = semicolonSplitter.split(contentType).iterator().next().toLowerCase();
			
			data.putString(Request.Headers.CONTENT_TYPE, contentType);
			
			if (contentType.startsWith("multipart/")) {
				multipartHandler.processData(request, data, contentType);
			} else {
				contentHandlers.getOrDefault(contentType, defaultContentHandler).processData(request, data, contentType);
			}
		}
		
		out.add(data);
	}
	
	private static interface RequestDataHandler {
		void processData(FullHttpRequest request, MutableData data, String contentType) throws Exception;
	}
	
	private static class JsonDataHandler implements RequestDataHandler {

		@Override
		public void processData(FullHttpRequest request, MutableData data, String contentType) throws Exception {
			try (InputStream content = new ByteBufInputStream(request.content())) {
				
				// TODO: need to fix this up, and the data stuff too! (MutableMemoryData.readJson(factory.createJsonParser(content)))
				
				@SuppressWarnings("unchecked")
				Map<String,Object> map = jsonMapper.readValue(content, Map.class);
				MutableData jsonData = MutableMemoryData.createFromMap(map);
				
				if (log.isDebugEnabled()) {
					log.debug("converted incoming json into map [{}] and data [{}]", map, jsonData.toPrettyJson());
				}
				
				jsonData.forEach(e -> {
					log.debug("huh? {} -> {}", e.getKey(), e.getValue());
				});
				
				jsonData.forEachContent((p, c) -> {
					log.debug("so, putting {} -> {}", Request.DATA.add(p), c);
					data.put(Request.DATA.add(p), c);
				});
				
			}
		
		}
	}
	
	private static class FormUrlEncodedDataHandler implements RequestDataHandler {

		@Override
		public void processData(FullHttpRequest request, MutableData data, String contentType) throws Exception {
			log.debug("got urlencoded form data");
			byte[] bytes = new byte[request.content().readableBytes()];
			request.content().readBytes(bytes);
			QueryStringDecoder formparams = new QueryStringDecoder("?" + new String(bytes));
			MutableData requestData = data.createMapAt(Request.DATA);
			
			for (Entry<String, List<String>> entry : formparams.parameters().entrySet()) {
				String name = entry.getKey();
				for (String value : entry.getValue()) {
					if (name.equals(FORM_FIELD_METHOD)) {
						data.putString(Request.METHOD, value.toString());
					} else {
						requestData.put(dots(name), utf8(value));
					}	
				}
			}	
		}
		
	}
	
	private static class MultipartDataHandler implements RequestDataHandler {

		@Override
		public void processData(FullHttpRequest request, MutableData data, String contentType) throws Exception {
			MutableData requestData = data.createMapAt(Path.Request.DATA);
			HttpPostRequestDecoder post = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
			int uploadCount = 0;
			for (InterfaceHttpData postdata : post.getBodyHttpDatas()) {
				log.debug("{} -> {}", postdata.getName(), postdata.getHttpDataType());
				switch (postdata.getHttpDataType()) {
				case FileUpload:
					FileUpload upload = (FileUpload) postdata;
					
					MutableData postItem = MutableMemoryData.create();
					postItem
						//.put(dots("name"), utf8(upload.getName()))
						.putString(dots("filename"), upload.getFilename())
						.put(dots("data"), binary(upload.getContentType(), upload.getByteBuf().retain().nioBuffer().asReadOnlyBuffer()));
											
					requestData.put(dots(postdata.getName()), postItem);
					uploadCount++;
					break;
				case Attribute:
					 Attribute attribute = (Attribute) postdata;
					 requestData.put(dots(attribute.getName()), utf8(attribute.getValue()));
					 uploadCount++;
					break;
				default:
					break;
				}
			}
			if (uploadCount == 0) {
				log.debug("ah no uploads, %d readable bytes..", request.content().readableBytes());
			} else {
				log.debug("{} upload(s)", uploadCount);
				requestData.forEachContent((path, content) -> {
					log.debug("  {} -> {}", path.dots(), content);
				});
			}
		}
		
	}
	
	private static class PlainTextDataHandler implements RequestDataHandler {

		@Override
		public void processData(FullHttpRequest request, MutableData data, String contentType) throws Exception {
			byte[] bytes = new byte[request.content().readableBytes()];
			request.content().readBytes(bytes);
			data.putString(Request.CONTENT, new String(bytes, StandardCharsets.UTF_8));
		}
		
	}
	
	private static class DefaultDataHandler implements RequestDataHandler {

		@Override
		public void processData(FullHttpRequest request, MutableData data, String contentType) throws Exception {
			byte[] bytes = new byte[request.content().readableBytes()];
			request.content().readBytes(bytes);
			data.put(Request.CONTENT, binary(contentType, bytes));
		}
		
	}

}
