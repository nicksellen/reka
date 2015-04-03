package reka.net.http.converters;

import static reka.util.Path.CONTENT;
import io.netty.handler.codec.http.FullHttpMessage;

import java.nio.charset.StandardCharsets;

import reka.data.MutableData;

public class PlainTextMessageToDataConverter implements HttpMessageToDataConverter {

	@Override
	public void processData(FullHttpMessage message, MutableData out, String contentType) throws Exception {
		byte[] bytes = new byte[message.content().readableBytes()];
		message.content().readBytes(bytes);
		out.putString(CONTENT, new String(bytes, StandardCharsets.UTF_8));
	}
	
}