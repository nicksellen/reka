package reka.config.processor;

import reka.config.Config;
import reka.config.parser.values.KeyAndSubkey;

public interface ConfigConverter {

	interface Output {
		void mark();
	    Output reset();
        Output add(Config config);
        Output add(Iterable<Config> configs);
		Output embed(Config config);
		Output key(KeyAndSubkey keyword);
		Output keyvalue(KeyAndSubkey key, String value);
		Output doc(KeyAndSubkey key, String type, byte[] content);
		Output doc(KeyAndSubkey key, Object value, String type, byte[] content);
        Output obj(KeyAndSubkey key, Config... children);
        Output obj(KeyAndSubkey key, Iterable<Config> children);
        Output obj(KeyAndSubkey key, Object value, Config... children);
		Output obj(KeyAndSubkey key, Object value, Iterable<Config> children);
		Output toplevel();
		boolean isTopLevel();
		int depth();
    }
	
	void convert(Config config, Output out);
	
}
