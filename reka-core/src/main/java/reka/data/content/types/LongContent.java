package reka.data.content.types;

import java.io.DataOutput;
import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import reka.data.content.Content;

import com.google.common.hash.Hasher;

public class LongContent implements Content {

	private final long value;
	
	public LongContent(long value) {
		this.value = value;
	}

	@Override
	public void writeJsonTo(JsonGenerator out) throws IOException {
		out.writeNumber(value);
	}

	@Override
	public void out(DataOutput out) throws IOException {
		out.writeLong(value);
	}

	@Override
	public Type type() {
		return Type.INTEGER;
	}
	
	@Override
	public String toString() {
		return Long.toString(value);
	}
	
	@Override
	public long asLong() {
		return value;
	}

	@Override
	public Object value() {
		return value;
	}

	@Override
	public Hasher hash(Hasher hasher) {
		return hasher.putLong(value);
	}
	
	@Override
	public String asUTF8() {
		return toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
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
		LongContent other = (LongContent) obj;
		if (value != other.value)
			return false;
		return true;
	}

	
}