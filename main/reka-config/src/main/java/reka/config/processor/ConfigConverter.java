package reka.config.processor;

import reka.config.Config;
import reka.config.parser.values.KeyVal;

public interface ConfigConverter {

	public interface Output {
		public void mark();
	    public Output reset();
        public Output add(Config config);
        public Output add(Iterable<Config> configs);
		public Output embed(Config config);
		public Output key(KeyVal keyword);
		public Output keyvalue(KeyVal key, String value);
		public Output doc(KeyVal key, String type, byte[] content);
		public Output doc(KeyVal key, Object value, String type, byte[] content);
        public Output obj(KeyVal key, Config... children);
        public Output obj(KeyVal key, Iterable<Config> children);
        public Output obj(KeyVal key, Object value, Config... children);
		public Output obj(KeyVal key, Object value, Iterable<Config> children);
		public Output toplevel();
		public boolean isTopLevel();
		public String[] path();
    }
	
	public void convert(Config config, Output out);
	
}
