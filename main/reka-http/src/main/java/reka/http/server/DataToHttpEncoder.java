package reka.http.server;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static reka.core.data.MoreDataUtils.writeToOutputStreamAsJson;
import static reka.core.data.MoreDataUtils.writeToOutputStreamAsPrettyJson;
import static reka.util.Util.unchecked;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.Data;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;

public class DataToHttpEncoder extends MessageToMessageEncoder<Data> {
	
	private static final String DEFAULT_SERVER_NAME = "reka-http";
	
	private static final byte[] NEW_LINE = "\n".getBytes(Charsets.UTF_8);
	
	private static final String TEXT_PLAIN = "text/plain";
	private static final String APPLICATION_JSON = "application/json";

	private final Logger logger = LoggerFactory.getLogger("http-encoder");
	
	@Override
	protected void encode(ChannelHandlerContext context, Data data, List<Object> out) throws Exception {
		
		try {
			HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(Integer.valueOf(data.getString(Response.STATUS).orElse("200")));
			
			Data maybeContent = data.at(Response.CONTENT);
			
			ByteBuf buffer = null;
			
			boolean headRequest = data.existsAt(Response.HEAD);
			
			if (headRequest) {
				logger.info("sending HEAD response");
			}

			File file = null;
			String contentType = null;
			
			if (maybeContent.isContent()) {
				
				if (!headRequest) { 
				
					Content content = maybeContent.content();
					
					switch (content.type()) {
					case UTF8: 
						buffer = context.alloc().buffer().writeBytes(content.asUTF8().getBytes(Charsets.UTF_8));
						break;
					case BINARY:
						if (content.hasFile()) {
							file = content.asFile();
						} else if (content.hasByteBuffer()) {
							buffer = Unpooled.wrappedBuffer(content.asByteBuffer());
						} else {
							buffer = Unpooled.wrappedBuffer(content.asBytes());
						}
						break;
					case DOUBLE:
						buffer = context.alloc().buffer().writeDouble(content.asDouble());
						break;
					case INTEGER:
						buffer = context.alloc().buffer().writeInt(content.asInt());
						break;
					case LONG:
						buffer = context.alloc().buffer().writeLong(content.asLong());
						break;
					case TRUE:
						buffer = context.alloc().buffer().writeBoolean(true);
						break;
					case FALSE:
						buffer = context.alloc().buffer().writeBoolean(false);
						break;
					case NULL:
						break;
					default:
						break;	
					}
				
				}
			} else if (maybeContent.isPresent()) {
				
				Data contentData = maybeContent;
				
				// send content data json
				buffer = context.alloc().buffer();
				
				if (data.existsAt(Request.Params.PRETTY)) {
					writeToOutputStreamAsPrettyJson(contentData, new ByteBufOutputStream(buffer));
					buffer.writeBytes(NEW_LINE);
				} else {
					writeToOutputStreamAsJson(contentData, new ByteBufOutputStream(buffer));
				}
				contentType = APPLICATION_JSON;
				
			} else if (!responseStatus.equals(HttpResponseStatus.NO_CONTENT)) {
				
				// 404
				FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				response.headers().set(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN);
				response.content().writeBytes("no page here!\n\n".getBytes(Charsets.UTF_8));
				response.content().writeBytes(data.toPrettyJson().getBytes(Charsets.UTF_8));
				out.add(response);
				return;
			}

			HttpResponse response;
			if (buffer != null) {
				response = new DefaultFullHttpResponse(HTTP_1_1, responseStatus, buffer);
			} else {
				response = new DefaultHttpResponse(HTTP_1_1, responseStatus);
			}
			
			response.headers().set(HttpHeaders.SERVER, DEFAULT_SERVER_NAME);
			io.netty.handler.codec.http.HttpHeaders.setDate(response, new Date());
			
			data.at(Response.HEADERS).forEachContent((p, c) -> {
				response.headers().set(p.last().toString(), c);
			});
			
			Collection<Cookie> cookies = new ArrayList<>();
			
			data.at(Response.COOKIES).forEachData((p, d) -> {
				String val = d.isContent() ? d.content().asUTF8() : d.getString("value").get();
				cookies.add(new DefaultCookie(p.toString(), val));
			});
			
			for (String c : ServerCookieEncoder.encode(cookies)) {
				response.headers().add(HttpHeaders.SET_COOKIE, c);
			}

			if (contentType != null) {
				response.headers().set(HttpHeaders.CONTENT_TYPE, contentType);
			}

			if (response.headers().get(HttpHeaders.CONTENT_LENGTH) == null) {
				if (file != null) {
					response.headers().add(HttpHeaders.CONTENT_LENGTH, file.length());
				} else if (buffer != null) {
					response.headers().add(HttpHeaders.CONTENT_LENGTH, buffer.readableBytes());				
				} else {
					response.headers().add(HttpHeaders.CONTENT_LENGTH, 0);
				}
			}
			
			out.add(response);
			
			if (file != null) {
				try {
					// TODO: handle the 'Range:' header here... :)
					out.add(new DefaultFileRegion(new FileInputStream(file).getChannel(), 0, file.length()));
					out.add(LastHttpContent.EMPTY_LAST_CONTENT);
				} catch (FileNotFoundException e) {
					throw unchecked(e); // not very good...
				}
			} else if (buffer == null) {
				out.add(LastHttpContent.EMPTY_LAST_CONTENT);
			}
		} catch (Throwable t) {
			logger.error("oops!", t);
		}
	}

}
