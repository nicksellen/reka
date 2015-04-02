package reka.config.parser.handlers;

import static java.lang.Character.isWhitespace;
import reka.config.parser.EatHandler;
import reka.config.parser.ParseContext;

class WhitespaceEater implements EatHandler {

	@Override
	public void eat(ParseContext ctx) {
		while (!ctx.isEOF() && isWhitespace(ctx.peekChar())) {
			ctx.popChar();
		}
	}
	
}