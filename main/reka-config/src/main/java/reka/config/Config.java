package reka.config;

import java.io.File;
import java.math.BigDecimal;

import reka.config.formatters.Formattable;
import reka.config.parser.Parser;
import reka.config.parser.values.KeyAndSubkey;

public interface Config extends Formattable {
	
	public static ConfigBuilder newBuilder() {
		return new ConfigBuilder();
	}
	
	boolean hasKey();
	String key();
	
	boolean hasSubkey();
	String subkey();
    
    Source source();
    
    boolean hasBody();
    ConfigBody body();
    
    boolean hasValue();
    Object value();
    String valueAsString();
    long valueAsLong();
    int valueAsInt();
    double valueAsDouble();
    Number valueAsNumber();
	BigDecimal valueAsBigDecimal();
    
    boolean hasDocument();
    String documentType();
    byte[] documentContent();
    String documentContentAsString();
    
    public static NavigableConfig fromFile(File file) {
		return fromSource(FileSource.from(file, file.getParentFile()));
	}
	
	public static NavigableConfig fromString(String content) {
		return fromSource(StringSource.from(content));
	}
	
	public static NavigableConfig fromSource(Source source) {
		return Parser.parse(source);
	}
	
	public default ConfigBuilder toBuilder() {
		ConfigBuilder builder = newBuilder();
		builder.source(source());
		if (hasKey()) builder.key(key());
		if (hasSubkey()) builder.subkey(subkey());
		if (hasValue()) builder.value(value());
		if (hasDocument()) builder.document(documentType(), documentContent());
		if (hasBody()) builder.body(body());
		return builder;
	}
	
	public static class ConfigBuilder {
		
	    private Source source;
	    private String key;
	    private String subkey;
	    private ConfigBody body;
	    private Object value;
	    private String documentType;
	    private byte[] documentContent;
	    
	    public ConfigBuilder() { }
	    
	    public Config build() {
	    	return new DefaultConfig(source, key, subkey, body, value, documentType, documentContent);
	    }
	    
	    public ConfigBuilder source(Source val) {
	    	source = val;
	    	return this;
	    }
	    
	    public ConfigBuilder key(String val) {
	    	key = val;
	    	return this;
	    }

		public ConfigBuilder keyAndSubkey(KeyAndSubkey val) {
			key = val.key();
			subkey = val.subkey();
			return this;
		}
	    
	    public ConfigBuilder subkey(String val) {
	    	subkey = val;
	    	return this;
	    }
	    
	    public ConfigBuilder document(String type, byte[] content) {
	    	documentType = type;
	    	documentContent = content;
	    	return this;
	    }
	    
	    public ConfigBuilder body(ConfigBody val) {
	    	body = val;
	    	return this;
	    }
	    
	    public ConfigBuilder value(Object val) {
	    	value = val;
	    	return this;
	    }

	}

    
}
