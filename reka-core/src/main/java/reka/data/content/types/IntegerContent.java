package reka.data.content.types;

import java.io.DataOutput;
import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import reka.data.content.Content;

import com.google.common.hash.Hasher;

public class IntegerContent implements Content {

	private final int value;
	
	public IntegerContent(int value) {
		this.value = value;
	}

	@Override
	public void writeJsonTo(JsonGenerator out) throws IOException {
		out.writeNumber(value);
	}

	@Override
	public void out(DataOutput out) throws IOException {
		out.writeInt(value);
	}

	@Override
	public Type type() {
		return Type.INTEGER;
	}
	
	@Override
	public String toString() {
		return Integer.toString(value);
	}
	
	@Override
	public int asInt() {
		return value;
	}

	@Override
	public Object value() {
		return value;
	}
	
	@Override
	public String asUTF8() {
		return toString();
	}

	@Override
	public Hasher hash(Hasher hasher) {
		return hasher.putInt(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
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
		IntegerContent other = (IntegerContent) obj;
		if (value != other.value)
			return false;
		return true;
	}
	
	
}