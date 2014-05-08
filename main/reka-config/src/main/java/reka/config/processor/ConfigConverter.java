package reka.config.processor;

import reka.config.Config;

public interface ConfigConverter {

	public interface Output {
		public void mark();
	    public Output reset();
        public Output add(Config config);
        public Output add(Iterable<Config> configs);
		public Output embed(Config config);
		public Output key(String keyword);
		public Output keyvalue(String key, String value);
		public Output doc(String key, String type, byte[] content);
		public Output doc(String key, Object value, String type, byte[] content);
        public Output obj(String key, Config... children);
        public Output obj(String key, Iterable<Config> children);
        public Output obj(String key, Object value, Config... children);
		public Output obj(String key, Object value, Iterable<Config> children);
		public Output toplevel();
		public boolean isTopLevel();
		public String[] path();
    }
	
	public void convert(Config config, Output out);
	
}
