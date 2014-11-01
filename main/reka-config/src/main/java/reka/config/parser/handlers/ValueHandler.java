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
		boolean arrayBody = false;
		
		while (!ctx.isEOF() && ctx.peekChar() != '\n') {
			sb.append(ctx.popChar());
			int docIdx = sb.indexOf("<<-");
			if (docIdx != -1) {
				sb.setLength(docIdx);
				doc = true;
				break;
			}
		} 

		String str = sb.toString().trim();
		
		if (str.length() > 0) {
			
			if (str.endsWith("{")) {
				str = str.substring(0, str.length() - 1);
				str = str.trim();
				body = true;
			}
			
			if (str.endsWith("[")) {
				str = str.substring(0, str.length() - 1);
				str = str.trim();
				arrayBody = true;
			}

			if (str.length() > 0) {
				ctx.emit("value", new ValueVal(str), 1, str.length());
			}
		}
		
		if (doc) {
			ctx.next(ParseHandlers.DOC);
		} else if (body) {
			ctx.eat(ParseHandlers.WHITESPACE);
			ctx.next(new BodyHandler(false));
		} else if (arrayBody) {
			ctx.eat(ParseHandlers.WHITESPACE);
			ctx.next(new ArrayBodyHandler(false));
		}
		
	}
	
}