package reka.net.http.converters;

import io.netty.handler.codec.http.FullHttpMessage;
import reka.data.MutableData;

public interface HttpMessageToDataConverter {
	void processData(FullHttpMessage message, MutableData out, String contentType) throws Exception;
}