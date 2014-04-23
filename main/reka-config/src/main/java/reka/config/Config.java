package reka.config;

import java.math.BigDecimal;

import reka.config.formatters.Formattable;

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
    
}
