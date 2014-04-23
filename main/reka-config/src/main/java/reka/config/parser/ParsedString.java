package reka.config.parser;

import org.parboiled.support.Position;

class ParsedString extends ParsedValue {

	private final String value;
	
	public ParsedString(Position pos, String value) {
		this.value = value;
	}

	@Override
	public Object val() {
		return value;
	}

}
