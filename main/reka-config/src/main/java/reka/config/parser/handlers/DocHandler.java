package reka.config.parser.handlers;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Character.isWhitespace;
import static reka.config.formatters.FormattingUtil.removeIndentation;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseHandler;
import reka.config.parser.values.DocVal;

import com.google.common.base.Charsets;

class DocHandler implements ParseHandler {
	
	public static DocHandler INSTANCE = new DocHandler();
	
	private DocHandler() { }

	@Override
	public void accept(ParseContext ctx) {
		
		StringBuffer sb = new StringBuffer();
		StringBuffer linebuffer = new StringBuffer();
		
		int current = ctx.endPos();
		
		// TODO: get the doc type first
		ctx.eat(ParseHandlers.SPACE);
		String contentType = ctx.parseSync(ParseHandlers.OPTIONAL_WORD).orElse("unknown");
		
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