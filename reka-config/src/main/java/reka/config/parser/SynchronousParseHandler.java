package reka.config.parser;

import java.util.function.Function;

public interface SynchronousParseHandler <T> extends Function<ParseContext, T> {
	
}
