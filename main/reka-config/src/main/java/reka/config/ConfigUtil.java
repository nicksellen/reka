package reka.config;

import reka.config.parser.values.KeyVal;


public class ConfigUtil {
    
    public static Config k(Source source, KeyVal key) {
        return new Keyword(source, key);
    }

    public static Config keyword(KeyVal key) {
        return new Keyword(null, key);
    }
    
    public static Config kv(Source source, KeyVal key, Object value) {
        return new KeyAndOptionalValue(source, key, value);
    }
    
    public static Config doc(Source source, KeyVal key, String type, byte[] content) {
        return new KeyAndDocument(source, key, null, type, content);
    }

    public static Config doc(Source source, KeyVal key, Object value, String type, byte[] content) {
        return new KeyAndDocument(source, key, value, type, content);
    }
    
    public static Config obj(Source source, KeyVal key, Object value, Iterable<Config> children) {
        return new KeyWithBody(source, key, value, children);
    }
    
}
