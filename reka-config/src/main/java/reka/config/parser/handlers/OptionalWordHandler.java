package reka.config.parser.handlers;

import static java.lang.Character.isWhitespace;

import java.util.Optional;

import reka.config.parser.ParseContext;
import reka.config.parser.SynchronousParseHandler;

class OptionalWordHandler implements SynchronousParseHandler<Optional<String>> {
	
	@Override
	public Optional<String> apply(ParseContext ctx) {
		StringBuilder sb = new StringBuilder();
		while (!ctx.isEOF() && !isWhitespace(ctx.peekChar())) {
			sb.append(ctx.popChar());
		}
		ctx.eat(ParseHandlers.SPACE);
		return sb.length() > 0 ? Optional.of(sb.toString()) : Optional.empty();
	}
	
}