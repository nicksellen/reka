package reka.api;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

public interface JsonProvider {

	static final JsonFactory jsonFactory = new JsonFactory();
	
	public void writeJsonTo(JsonGenerator json) throws IOException;
	
	default public String toJson() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JsonGenerator json = jsonFactory.createJsonGenerator(baos);
			writeJsonTo(json);
			json.flush();
			return new String(baos.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw unchecked(e);
		}	
	}
}

