package reka.config.parser2.states;

import java.util.ArrayList;
import java.util.List;

import reka.config.parser2.ParseContext;
import reka.config.parser2.ParseState;
import reka.config.parser2.Parser.BodyVal;
import reka.config.parser2.Parser.KeyAndValueItem;

public class BodyState implements ParseState {
	
	private final List<KeyAndValueItem> items = new ArrayList<>();

	public void receive(KeyAndValueItem item) {
		items.add(item);
	}
	
	@Override
	public void accept(ParseContext ctx) {
		
		while (!ctx.isEOF()) {
			char c = ctx.peekChar();
			switch (c) {
			case '}':
				ctx.popChar();
				break;
			default:
				ctx.take(new ItemState());
			}
		}
		
		ctx.emit("body", new BodyVal(items));
	}
	
}