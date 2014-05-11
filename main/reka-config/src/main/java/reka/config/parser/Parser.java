package reka.config.parser;

import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.parser.states.BodyState;

public class Parser {
	
	public static ConfigBody parse(Source source) {
		BodyState root = new BodyState();
		new ParseContext(source, root).run();
		return ConfigBody.of(source, root.configs());
	}

}
