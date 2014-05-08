package reka.api;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

public interface JsonProvider {
	public void writeJsonTo(JsonGenerator json) throws IOException;
}

