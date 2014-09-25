package reka.api.content.types;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.codehaus.jackson.JsonGenerator;

import reka.api.content.Content;

import com.google.common.hash.Hasher;

public class UTF8Content implements Content {
	
	private final String value;
	
	public UTF8Content(String value) {
		this.value = value;
	}

	@Override
	public void writeJsonTo(JsonGenerator out) throws IOException {
		out.writeString(value);
	}

	@Override
	public void out(DataOutput out) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	@Override
	public Type type() {
		return Type.UTF8;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	@Override
	public String asUTF8() {
		return value;
	}

	@Override
	public Object value() {
		return value;
	}

	@Override
	public Hasher hash(Hasher hasher) {
		return hasher.putString(value, StandardCharsets.UTF_8);
	}

	@Override
	public boolean isEmpty() {
		return value.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UTF8Content other = (UTF8Content) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
}