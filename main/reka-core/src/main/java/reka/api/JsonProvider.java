package reka.api;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

public interface JsonProvider {
	public void out(JsonGenerator json) throws IOException;
}

