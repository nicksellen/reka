package reka.api.content.types;

import java.io.DataOutput;
import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import reka.api.content.Content;

import com.google.common.hash.Hasher;

public class DoubleContent implements Content {

	private final double value;
	
	public DoubleContent(double value) {
		this.value = value;
	}
	
	@Override
	public Type type() {
		return Type.DOUBLE;
	}

	@Override
	public void writeJsonTo(JsonGenerator json) throws IOException {
		json.writeNumber(value);
	}

	@Override
	public void out(DataOutput out) throws IOException {
		out.writeDouble(value);
	}

	@Override
	public double asDouble() {
		return value;
	}
	
	@Override
	public Object value() {
		return value;
	}

	@Override
	public Hasher hash(Hasher hasher) {
		return hasher.putDouble(value);
	}

	@Override
	public String asUTF8() {
		return toString();
	}
	
	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		DoubleContent other = (DoubleContent) obj;
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
			return false;
		return true;
	}
	
}