package reka.config;

import reka.config.parser.values.KeyAndSubkey;

class KeyWithBody extends KeyAndOptionalValue implements Config {

    private final ConfigBody body; 
    
    KeyWithBody(Source source, KeyAndSubkey key, Object value, Iterable<Config> children) {
        super(source, key, value);
        this.body = ConfigBody.of(source, children);
    }
    
    @Override
    public boolean hasBody() {
        return true;
    }
    
    @Override
    public ConfigBody body() {
        return body;
    }
    
}
