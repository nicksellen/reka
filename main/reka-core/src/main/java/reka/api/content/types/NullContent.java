package reka.api.content.types;

import java.io.DataOutput;
import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import reka.api.content.Content;

import com.google.common.hash.Hasher;

public class NullContent implements Content {

	public static final NullContent INSTANCE = new NullContent();
	
	private NullContent() {}
	
	@Override
	public Type type() {
		return Type.NULL;
	}

	@Override
	public void writeJsonTo(JsonGenerator json) throws IOException {
		json.writeNull();
	}

	@Override
	public void out(DataOutput out) throws IOException {
	}

	@Override
	public Object value() {
		return null;
	}

	@Override
	public Hasher hash(Hasher hasher) {
		return hasher.putByte((byte)0);
	}

	@Override
	public boolean isEmpty() {
		return true;
	}
	
}