package reka.http.server;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.trueValue;
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
import java.net.InetSocketAddress;
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
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

@Sharable
public class FullHttpToDatasetDecoder extends MessageToMessageDecoder<FullHttpRequest> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();
		
	private static final String FORM_FIELD_METHOD = "_method";
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) throws Exception {
		
		if (request.getUri().equals("/favicon.ico")) {
			return;
		}
		
		final MutableData data = MutableMemoryData.create();

		MutableData params = data.createMapAt(Request.PARAMS);
		MutableData headers = data.createMapAt(Request.HEADERS);
		MutableData cookies = data.createMapAt(Request.COOKIES);
		
		QueryStringDecoder qs = new QueryStringDecoder(request.getUri());
		
		String host = hostSplitter.split(HttpHeaders.getHost(request)).iterator().next();

		InetSocketAddress local = (InetSocketAddress) ctx.channel().localAddress();
		
		int port = local.getPort();
		StringBuilder sb = new StringBuilder().append(host);
		if (port != 80) {
			sb.append(':').append(port);
		}
		String fullHostname = sb.toString();
		data.putString(path(PathElements.name("something")), fullHostname);
		
		data.putString(Request.PATH, QueryStringDecoder.decodeComponent(qs.path()))
			.putString(Request.HOST, host);
		
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
		
		String httpMethod = request.getMethod().toString(); // may be overridden by param later

		//log.info("{} {}{}", httpMethod, fullHostname, request.getUri());
		
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
		
		if (request.getMethod().equals(HttpMethod.POST) || request.getMethod().equals(HttpMethod.PUT)) {
			
			String requestContentType = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
			if (requestContentType == null) requestContentType = "application/octet-stream";
			
			logger.debug("requestContentType: [{}]", requestContentType);
			
			if ("application/json".equals(requestContentType)) {
				try (InputStream content = new ByteBufInputStream(request.content())) {
					
					// TODO: need to fix this up, and the data stuff too!
					
					@SuppressWarnings("unchecked")
					Map<String,Object> map = jsonMapper.readValue(content, Map.class);
					MutableData jsonData = MutableMemoryData.createFromMap(map);
					log.debug("converted incoming json into map [{}] and data [{}]", map, jsonData.toPrettyJson());
					
					jsonData.forEach(e -> {
						log.debug("huh? {} -> {}", e.getKey(), e.getValue());
					});
					
					jsonData.forEachContent((p, c) -> {
						log.debug("so, putting {} -> {}", Request.DATA.add(p), c);
						data.put(Request.DATA.add(p), c);
					});
					
//					MutableMemoryData.createFromMap(//mapper.conv)
					//data.createMapAt(Request.DATA)
						//.merge(MutableMemoryData.readJson(factory.createJsonParser(content)));
				}
				
			} else if ("text/plain".equals(requestContentType)) {
				byte[] bytes = new byte[request.content().readableBytes()];
				request.content().readBytes(bytes);
				data.putString(Request.CONTENT, new String(bytes, Charsets.UTF_8));
			
			} else if ("application/x-www-form-urlencoded".equals(requestContentType)) {
				logger.debug("got urlencoded form data");
				byte[] bytes = new byte[request.content().readableBytes()];
				request.content().readBytes(bytes);
				QueryStringDecoder formparams = new QueryStringDecoder("?" + new String(bytes));
				MutableData requestData = data.createMapAt(Request.DATA);
				
				for (Entry<String, List<String>> entry : formparams.parameters().entrySet()) {
					String name = entry.getKey();
					for (String value : entry.getValue()) {
						if (name.equals(FORM_FIELD_METHOD)) {
							httpMethod = value.toString();
						} else {
							requestData.put(dots(name), utf8(value));
						}	
					}
				}
				
			} else if (requestContentType != null && requestContentType.startsWith("multipart/")) {

				HttpPostRequestDecoder post = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
				MutableData requestData = data.createMapAt(Path.Request.DATA);
				int uploadCount = 0;
				for (InterfaceHttpData postdata : post.getBodyHttpDatas()) {
					logger.debug("{} -> {}", postdata.getName(), postdata.getHttpDataType());
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
					logger.debug("ah no uploads, %d readable bytes..", request.content().readableBytes());
				} else {
					logger.debug("{} upload(s)", uploadCount);
					requestData.forEachContent((path, content) -> {
						logger.debug("  {} -> {}", path.dots(), content);
					});
				}
			} else {
				logger.debug("content type is [{}]", requestContentType);
				byte[] bytes = new byte[request.content().readableBytes()];
				request.content().readBytes(bytes);
				data.put(Request.CONTENT, binary(requestContentType, bytes));
			}
			
		}
		
		if ("HEAD".equals(httpMethod)) {
			httpMethod = "GET";
			data.put(Response.HEAD, trueValue());
		}
		
		data.putString(Request.METHOD, httpMethod);
		
		out.add(data);
	}

}
