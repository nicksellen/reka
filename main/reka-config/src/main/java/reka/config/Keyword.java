package reka.config;

import reka.config.parser.values.KeyVal;


public class Keyword extends AbstractConfig {

    private final KeyVal key;
    
    public Keyword(Source source, KeyVal key) {
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
