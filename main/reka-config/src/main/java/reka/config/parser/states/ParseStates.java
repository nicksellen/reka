package reka.config.parser.states;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Character.isWhitespace;
import static reka.config.formatters.FormattingUtil.removeIndentation;

import java.util.Optional;

import reka.config.parser.EatHandler;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseState;
import reka.config.parser.SimpleParseHandler;
import reka.config.parser.values.DocVal;
import reka.config.parser.values.KeyVal;
import reka.config.parser.values.ValueVal;

import com.google.common.base.Charsets;

final class ParseStates {
	
	public static final ValueState VALUE = new ValueState();
	public static final DocState DOC = new DocState();
	public static final WhitespaceEater WHITESPACE = new WhitespaceEater();
	public static final SpaceEater SPACE = new SpaceEater();
	
	public static final SimpleParseHandler<KeyVal> KEY = new KeyHandler();
	public static final SimpleParseHandler<Optional<String>> OPTIONAL_WORD = new OptionalWordHandler();
	
	private static class KeyHandler implements SimpleParseHandler<KeyVal> {

		@Override
		public KeyVal apply(ParseContext ctx) {
			StringBuilder sb = new StringBuilder();
			while (!ctx.isEOF() && !isWhitespace(ctx.peekChar())) {
				sb.append(ctx.popChar());
			}
			ctx.eat(SPACE);
			return new KeyVal(sb.toString());
		}
		
	}
	
	private static class OptionalWordHandler implements SimpleParseHandler<Optional<String>> {

		@Override
		public Optional<String> apply(ParseContext ctx) {
			StringBuilder sb = new StringBuilder();
			while (!ctx.isEOF() && !isWhitespace(ctx.peekChar())) {
				sb.append(ctx.popChar());
			}
			ctx.eat(SPACE);
			return sb.length() > 0 ? Optional.of(sb.toString()) : Optional.empty();
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
	
	private static class SpaceEater implements EatHandler {

		@Override
		public void eat(ParseContext ctx) {
			while (!ctx.isEOF() && ctx.peekChar() == ' ' || ctx.peekChar() == '\t') {
				ctx.popChar();
			}
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
			
			if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '{') {
				sb.setLength(sb.length() - 1);
				body = true;
			}
			
			if (sb.length() > 0) {
				String str = sb.toString().trim();
				ctx.emit("value", new ValueVal(str), 1, str.length());
			}
			
			if (doc) {
				ctx.next(ParseStates.DOC);
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
			
			int current = ctx.endPos();
			
			// TODO: get the doc type first
			ctx.eat(SPACE);
			String contentType = ctx.simpleParse(OPTIONAL_WORD).orElse("unknown");
			
			int offset = ctx.endPos() - current;
			
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
							String str = removeIndentation(sb.toString());
							ctx.emit("doc", new DocVal(contentType, str.getBytes(Charsets.UTF_8)), offset + 1, str.length());
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