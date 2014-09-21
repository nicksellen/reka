package reka.config;

import reka.config.parser.values.KeyAndSubkey;


public class Keyword extends AbstractConfig {

    private final KeyAndSubkey key;
    
    public Keyword(Source source, KeyAndSubkey key) {
        super(source);
        this.key = key;
    }
    
    @Override
    public String key() {
        return key.key();
    }
    
    @Override
    public String subkey() {
    	return key.subkey();
    }
    
}
