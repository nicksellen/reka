package reka.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.parser.values.KeyAndSubkey;

public class KeyAndOptionalValue extends AbstractConfig implements Config {

    private final KeyAndSubkey key;
    private final Object value;
    
    private final Pattern NUMBER = Pattern.compile("\\-?[0-9]+(\\.[0-9]+)?"); 
    
    public KeyAndOptionalValue(Source source, KeyAndSubkey key, Object value) {
        super(source);
        this.key = key;
        this.value = value;
    }
    
    @Override
    public String key() {
        return key.key();
    }
    
    @Override
    public String subkey() {
        return key.subkey();
    }
    
    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public Object value() {
        return value;
    }
    
    @Override
    public String valueAsString() {
        return hasValue() ? value.toString() : super.valueAsString();
    }
    
    public Number valueAsNumber() {
    	if (value instanceof Number) {
    		return (Number) value;
    	} else if (value instanceof String) {
    		String str = (String) value;
			Matcher match = NUMBER.matcher(str);
			if (match.matches()) {
				if (match.group(1) != null) {
					return new BigDecimal(str);
				} else {
					BigInteger big = new BigInteger(str);
					if (big.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) < 0) {
						return big.intValue();
					} else if (big.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0) {
						return big.longValue();
					} else {
						return big;
					}
				}
			}
		}
    	throw new RuntimeException(String.format("can't make a number from [%s] (%s)", value, value != null ? value.getClass() : null));
    }
    
    @Override
    public BigDecimal valueAsBigDecimal() {
    	if (value instanceof BigDecimal) {
    		return (BigDecimal) value;
    	} else if (value instanceof String) {
    		return new BigDecimal((String) value);
    	} else {
    		throw new RuntimeException(String.format("[%s] (%s) cannot be made as a BigDecimal", 
    				value, value != null ? value.getClass() : "null"));
    	}
    }
    
    @Override
    public int valueAsInt() {
    	return valueAsNumber().intValue();
    }
    
    @Override
    public long valueAsLong() {
    	return valueAsNumber().longValue();
    }
    
    @Override
    public double valueAsDouble() {
    	return valueAsNumber().doubleValue();
    }
    
    
}

