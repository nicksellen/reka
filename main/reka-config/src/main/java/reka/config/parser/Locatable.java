package reka.config.parser;

interface Locatable {
	public void sourceLocation(ParsedSourceLocation location);
	public ParsedSourceLocation sourceLocation();
}
