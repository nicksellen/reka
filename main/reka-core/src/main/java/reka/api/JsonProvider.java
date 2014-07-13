package reka.api;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Charsets;

public interface JsonProvider {

	static final JsonFactory jsonFactory = new JsonFactory();
	static final ObjectMapper jsonMapper = new ObjectMapper();
	
	public void writeJsonTo(JsonGenerator json) throws IOException;
	
	default public String toJson() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JsonGenerator json = jsonFactory.createJsonGenerator(baos);
			writeJsonTo(json);
			json.flush();
			return new String(baos.toByteArray(), Charsets.UTF_8);
		} catch (IOException e) {
			throw unchecked(e);
		}	
	}
}

