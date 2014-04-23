package reka.config.parser;

class ParsedSourceLocation {

	private final int pos;
	private final int length;
	
	public static ParsedSourceLocation create(int pos, int length) {
		return new ParsedSourceLocation(pos, length);
	}
	
	private ParsedSourceLocation(int pos, int length) {
		this.pos = pos;
		this.length = length;
	}
	
	public int start() {
		return pos;
	}
	
	public int length() {
		return length;
	}
	
}
