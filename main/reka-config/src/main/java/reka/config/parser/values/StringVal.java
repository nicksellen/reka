package reka.config.parser.values;

import static java.lang.String.format;

public abstract class StringVal {
	
	private final String value;

	public StringVal(String value) {
		this.value = value;
	}
	
	public String value() {
		return value;
	}
	
	@Override
	public String toString() {
		return format("%s('%s')", getClass().getSimpleName(), value);
	}
	
}