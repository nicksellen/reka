package reka.config.parser.handlers;

import reka.config.parser.ParseContext;
import reka.config.parser.ParseHandler;
import reka.config.parser.values.ValueVal;

class ValueHandler implements ParseHandler {

	public static ValueHandler INSTANCE = new ValueHandler();
	
	private ValueHandler() { }
	
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
			ctx.next(ParseHandlers.DOC);
		} else if (body) {
			ctx.eat(ParseHandlers.WHITESPACE);
			ctx.next(new BodyHandler(false));
		}
		
	}
	
}