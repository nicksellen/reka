package reka.config;

import static com.google.common.base.Preconditions.checkNotNull;
import reka.config.parser.values.KeyAndSubkey;

public class KeyAndDocument extends KeyAndOptionalValue {
    
    private final String type;
    private final byte[] content;

    public KeyAndDocument(Source source, KeyAndSubkey key, Object value, String type, byte[] content) {
        super(source, key, value);
    	checkNotNull(type);
        this.type = type;
        this.content = content;
    }
    
    @Override
    public boolean hasDocument() {
        return true;
    }
    
    @Override
    public String documentType() {
        return type;
    }
    
    @Override
    public byte[] documentContent() {
        return content;
    }

}
