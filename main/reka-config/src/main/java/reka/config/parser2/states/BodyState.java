package reka.config.parser2.states;

import static java.lang.Character.isWhitespace;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.parser2.ParseContext;
import reka.config.parser2.ParseState;
import reka.config.parser2.Parser2.BodyVal;

public class BodyState implements ParseState {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
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
				ctx.parse(new ConfigItemState());
			}
		}
		
		
	}
	
	public List<Config> configs() {
		return configs;
	}
	
}