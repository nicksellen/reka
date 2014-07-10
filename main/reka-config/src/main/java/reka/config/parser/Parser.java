package reka.config.parser;

import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.parser.handlers.BodyHandler;

public class Parser {
	
	public static ConfigBody parse(Source source) {
		BodyHandler root = new BodyHandler();
		new ParseContext(source, root).run();
		return ConfigBody.of(source, root.configs());
	}

}
