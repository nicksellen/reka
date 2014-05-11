package reka.config.parser.values;

import static java.lang.String.format;

public abstract class ByteVal {
	
	private final byte[] value;

	public ByteVal(byte[] value) {
		this.value = value;
	}
	
	public byte[] value() {
		return value;
	}
	
	@Override
	public String toString() {
		return format("%s('%s')", getClass().getSimpleName(), value);
	}
	
}