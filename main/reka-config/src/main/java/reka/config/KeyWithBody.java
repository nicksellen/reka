package reka.config;

class KeyWithBody extends KeyAndOptionalValue implements Config {

    private final ConfigBody body; 
    
    KeyWithBody(Source source, String key, Object value, Iterable<Config> children) {
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
