package reka.config.parser2.states;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Character.isWhitespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.parser2.EatHandler;
import reka.config.parser2.ParseContext;
import reka.config.parser2.ParseState;
import reka.config.parser2.Parser.DocVal;
import reka.config.parser2.Parser.KeyVal;
import reka.config.parser2.Parser.ValueVal;
import reka.config.parser2.SimpleParseHandler;

final class Stateless {
	
	private static final Logger log = LoggerFactory.getLogger(Stateless.class);
	
	public static final KeyState KEY = new KeyState();
	public static final ValueState VALUE = new ValueState();
	public static final DocState DOC = new DocState();
	public static final WhitespaceEater WHITESPACE = new WhitespaceEater();
	
	public static final SimpleParseHandler<KeyVal> KEY2 = new KeyHandler();
	
	private static class KeyHandler implements SimpleParseHandler<KeyVal> {

		@Override
		public KeyVal parse(ParseContext ctx) {
			StringBuilder sb = new StringBuilder();
			while (!ctx.isEOF() && !isWhitespace(ctx.peekChar())) {
				sb.append(ctx.popChar());
			}
			return new KeyVal(sb.toString());
		}
		
	}
	
	private static class WhitespaceEater implements EatHandler {

		@Override
		public void eat(ParseContext ctx) {
			while (!ctx.isEOF() && isWhitespace(ctx.peekChar())) {
				ctx.popChar();
			}
		}
		
	}
	
	private static class KeyState implements ParseState {

		@Override
		public void accept(ParseContext ctx) {

			StringBuilder sb = new StringBuilder();
			
			while (!ctx.isEOF() && !isWhitespace(ctx.peekChar())) {
				sb.append(ctx.popChar());
			}
			
			ctx.emit("key", new KeyVal(sb.toString()));
			
			ctx.eat(Stateless.WHITESPACE);
			
		}
		
	}
	
	private static class ValueState implements ParseState {
		
		@Override
		public void accept(ParseContext ctx) {

			StringBuffer sb = new StringBuffer();
			
			boolean doc = false;
			boolean body = false;
			
			while (!ctx.isEOF() && ctx.peekChar() != '\n') {
				sb.append(ctx.popChar());
				int docIdx = sb.indexOf("<<-");
				if (docIdx != -1) {
					sb.setLength(docIdx);
					doc = true;
					break;
				}
			}
			
			if (sb.charAt(sb.length() - 1) == '{') {
				sb.setLength(sb.length() - 1);
				body = true;
			}
			
			if (sb.length() > 0) {
				String str = sb.toString().trim();
				ctx.emit("value", new ValueVal(str), 1, str.length());
			}
			
			if (doc) {
				ctx.next(Stateless.DOC);
			} else if (body) {
				ctx.eat(WHITESPACE);
				ctx.next(new BodyState());
			}
			
		}
		
	}

	private static class DocState implements ParseState {

		@Override
		public void accept(ParseContext ctx) {
			
			StringBuffer sb = new StringBuffer();
			StringBuffer linebuffer = new StringBuffer();
			
			// TODO: get the doc type first
			int offset = ctx.eatUpTo('\n');
			
			boolean freshLine = true;
			int inEnd = 0;
			boolean foundEnd = false;
			while (!ctx.isEOF()) {
				char c = ctx.popChar();
				
				if (c == '\n') {
					sb.append(linebuffer);
					freshLine = true;
					linebuffer.setLength(0);
				}
	
				linebuffer.append(c);
				
				if (freshLine) {
					if (c == '-') {
						inEnd++;
						if (inEnd == 3) {
							foundEnd = true;
							String str = sb.toString();
							ctx.emit("doc", new DocVal(str), offset + 1, str.length());
							log.info("finished doc!");
							break;
						}
					} else {
						if (inEnd > 0 || !isWhitespace(c)) {
							freshLine = false;
						}
					}
				}
			}
			
			checkState(foundEnd, "couldn't find end of doc");
		}
		
	}
	
}