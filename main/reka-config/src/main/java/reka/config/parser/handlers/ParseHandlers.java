package reka.config.parser.handlers;

import static java.lang.Character.isWhitespace;

import java.util.Optional;

import reka.config.parser.EatHandler;
import reka.config.parser.ParseContext;
import reka.config.parser.SynchronousParseHandler;
import reka.config.parser.values.KeyVal;

final class ParseHandlers {
	
	public static final ValueHandler VALUE = ValueHandler.INSTANCE;
	public static final DocHandler DOC = DocHandler.INSTANCE;
	public static final WhitespaceEater WHITESPACE = new WhitespaceEater();
	public static final SpaceEater SPACE = new SpaceEater();
	
	public static final SynchronousParseHandler<KeyVal> KEY = new KeyHandler();
	public static final SynchronousParseHandler<Optional<String>> OPTIONAL_WORD = new OptionalWordHandler();
	
	private static class KeyHandler implements SynchronousParseHandler<KeyVal> {

		@Override
		public KeyVal apply(ParseContext ctx) {
			StringBuilder sb = new StringBuilder();
			while (!ctx.isEOF() && !isWhitespace(ctx.peekChar())) {
				sb.append(ctx.popChar());
			}
			ctx.eat(SPACE);
			return KeyVal.parse(sb.toString());
		}
		
	}
	
	private static class SpaceEater implements EatHandler {

		@Override
		public void eat(ParseContext ctx) {
			while (!ctx.isEOF() && (ctx.peekChar() == ' ' || ctx.peekChar() == '\t')) {
				ctx.popChar();
			}
		}
		
	}
	
}