package reka.config;

import java.io.File;
import java.math.BigDecimal;

import reka.config.formatters.Formattable;
import reka.config.parser2.Parser2;

public interface Config extends Formattable {
	
	boolean hasKey();
	String key();
    
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
    
    boolean hasData();
    String dataType();
    byte[] data();
    
    public static NavigableConfig fromFile(File file) {
		return fromSource(FileSource.from(file));
	}
	
	public static NavigableConfig fromString(String content) {
		return fromSource(StringSource.from(content));
	}
	
	public static NavigableConfig fromSource(Source source) {
		return Parser2.parse(source);
	}
    
}
