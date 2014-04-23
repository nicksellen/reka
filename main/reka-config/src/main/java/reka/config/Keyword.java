package reka.config;

public class Keyword extends AbstractConfig {

    private final String key;
    
    public Keyword(Source source, String key) {
        super(source);
        this.key = key;
    }
    
    @Override
    public String key() {
        return key;
    }
    
}
