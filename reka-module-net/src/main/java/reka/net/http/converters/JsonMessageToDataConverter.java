package reka.net.http.converters;

import static reka.util.Path.CONTENT;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpMessage;

import java.io.InputStream;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;

public class JsonMessageToDataConverter implements HttpMessageToDataConverter {
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();

	@Override
	public void processData(FullHttpMessage message, MutableData out, String contentType) throws Exception {
		try (InputStream content = new ByteBufInputStream(message.content())) {
			
			// TODO: need to fix this up, and the data stuff too! (MutableMemoryData.readJson(factory.createJsonParser(content)))
			
			@SuppressWarnings("unchecked")
			Map<String,Object> map = jsonMapper.readValue(content, Map.class);
			MutableData jsonData = MutableMemoryData.createFromMap(map);
			
			jsonData.forEachContent((p, c) -> {
				out.put(CONTENT.add(p), c);
			});
			
		}
	
	}
}