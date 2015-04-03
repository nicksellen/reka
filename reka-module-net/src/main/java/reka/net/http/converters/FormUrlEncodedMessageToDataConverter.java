package reka.net.http.converters;

import static reka.data.content.Contents.utf8;
import static reka.util.Path.CONTENT;
import static reka.util.Path.METHOD;
import static reka.util.Path.dots;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map.Entry;

import reka.data.MutableData;

public class FormUrlEncodedMessageToDataConverter implements HttpMessageToDataConverter {

	private static final String FORM_FIELD_METHOD = "_method";

	@Override
	public void processData(FullHttpMessage message, MutableData out, String contentType) throws Exception {
		byte[] bytes = new byte[message.content().readableBytes()];
		message.content().readBytes(bytes);
		QueryStringDecoder formparams = new QueryStringDecoder("?" + new String(bytes));
		MutableData requestData = out.createMapAt(CONTENT);
		for (Entry<String, List<String>> entry : formparams.parameters().entrySet()) {
			String name = entry.getKey();
			for (String value : entry.getValue()) {
				if (name.equals(FORM_FIELD_METHOD)) {
					out.putString(METHOD, value.toString());
				} else {
					requestData.put(dots(name), utf8(value));
				}	
			}
		}	
	}
	
}