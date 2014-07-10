package reka.config.parser.states;

import static java.lang.Character.isWhitespace;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseState;
import reka.config.parser.values.BodyVal;

public class BodyState implements ParseState {
	
	private final List<Config> configs = new ArrayList<>();

	public void receive(Config config) {
		configs.add(config);
	}
	
	@Override
	public void accept(ParseContext ctx) {
		
		while (!ctx.isEOF()) {
			char c = ctx.peekChar();
			if (isWhitespace(c)) {
				ctx.popChar();
			} else if (c == '}') {
				ctx.popChar();
				ctx.emit("body", new BodyVal(configs));
				break;
			} else {
				ctx.parse(new ItemState());
			}
		}		
		
	}
	
	public List<Config> configs() {
		return configs;
	}
	
}