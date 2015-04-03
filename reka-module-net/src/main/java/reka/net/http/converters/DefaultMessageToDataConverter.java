package reka.net.http.converters;

import static reka.data.content.Contents.binary;
import static reka.util.Path.CONTENT;
import io.netty.handler.codec.http.FullHttpMessage;
import reka.data.MutableData;

public class DefaultMessageToDataConverter implements HttpMessageToDataConverter {

	@Override
	public void processData(FullHttpMessage message, MutableData out, String contentType) throws Exception {
		byte[] bytes = new byte[message.content().readableBytes()];
		message.content().readBytes(bytes);
		out.put(CONTENT, binary(contentType, bytes));
	}
	
}