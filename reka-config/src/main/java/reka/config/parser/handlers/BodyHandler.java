package reka.config.parser.handlers;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Character.isWhitespace;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseHandler;
import reka.config.parser.values.BodyVal;

public class BodyHandler implements ParseHandler {
	
	private final boolean isRoot;
	
	public BodyHandler(boolean isRoot) {
		this.isRoot = isRoot;
	}
	
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
				checkState(!isRoot, "unexpected }");
				ctx.popChar();
				ctx.emit("body", new BodyVal(configs));
				break;
			} else {
				ctx.parse(new ItemHandler());
			}
		}		
		
	}
	
	public List<Config> configs() {
		return configs;
	}
	
}