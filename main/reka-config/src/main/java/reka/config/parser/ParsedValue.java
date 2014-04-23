package reka.config.parser;

abstract class ParsedValue implements Locatable {
	
	private ParsedSourceLocation pos;
	
	public abstract Object val();
	
	@Override
	public void sourceLocation(ParsedSourceLocation pos) {
		this.pos = pos;
	}
	
	public ParsedSourceLocation sourceLocation() {
		return pos;
	}
}
