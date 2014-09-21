package reka.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.formatters.ConfigFormatter;
import reka.config.formatters.Formatter;

import com.google.common.base.Charsets;

public class DefaultConfig implements Config {
	
	private final Pattern NUMBER = Pattern.compile("\\-?[0-9]+(\\.[0-9]+)?");
	
    private final Source source;
    private final String key;
    private final String subkey;
    private final ConfigBody body;
    private final Object value;
    private final String documentType;
    private final byte[] documentContent;
    
	public DefaultConfig(Source source, String key, String subkey,
			ConfigBody body, Object value, String documentType,
			byte[] documentContent) {
		this.source = source;
		this.key = key;
		this.subkey = subkey;
		this.body = body;
		this.value = value;
		this.documentType = documentType;
		this.documentContent = documentContent;
	}

	@Override
	public String key() {
		checkNotNull(key, "missing key");
		return key;
	}

	@Override
	public String subkey() {
		checkNotNull(subkey, "missing subkey");
		return subkey;
	}
    
    @Override
    public boolean hasKey() {
    	return key != null;
    }
    
    @Override
    public boolean hasSubkey() {
    	return subkey != null;
    }
    
    @Override
    public Source source() {
        return source;
    }

    @Override
    public boolean hasBody() {
        return body != null;
    }
    
    @Override
    public ConfigBody body() {
        return body;
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
    public boolean hasDocument() {
        return documentContent != null;
    }

    @Override
    public String documentType() {
    	checkNotNull(documentType);
        return documentType;
    }

    @Override
    public byte[] documentContent() {
    	checkNotNull(documentContent);
        return documentContent;
    }
    
    @Override
    public BigDecimal valueAsBigDecimal() {
    	checkNotNull(value, "value is missing");
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
    public Number valueAsNumber() {
    	checkNotNull(value, "value is missing");
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
    
    @Override
    public String valueAsString() {
    	checkNotNull(value, "value is missing");
    	return value.toString();
    }
    
    @Override
    public String documentContentAsString() {
    	checkState(hasDocument(), "this config does not have a document");
    	return new String(documentContent(), Charsets.UTF_8);
    }
    
    @Override
    public <T> T format(Formatter<T> out, FormattingOptions opts) {
        formatted(out, opts);
        return out.format();
    }

    @Override
    public <T> T format(Formatter<T> out) {
    	return format(out, new FormattingOptions());
    }
    

    @Override
    public String format(FormattingOptions opts) {
        return format(new ConfigFormatter(), opts);
    }
    
    @Override
    public String format() {
    	return format(new FormattingOptions());
    }
    
    @Override
    public String toString() {
    	return format();
    }
    
    private void formatted(Formatter<?> out, FormattingOptions opts) {
        
        out.startEntry(key(), hasBody());
        
        if (hasValue()) out.value(valueAsString());
        
        if (hasDocument()) {
            out.document(documentType(), documentContent());
        } else if (hasBody()) {
            if (body().elementCount() > 0) {
                out.startChildren(body().elementCount());
                formattedBody(out, opts);
                out.endChildren();
            } else {
                out.noChildren();
            }
        }
        
        out.endEntry();
    }
    

    private void formattedBody(Formatter<?> out, FormattingOptions opts) {
        if (hasBody()) {
            for (Config child : body()) {
                child.format(out, opts);
            }
        }
    }
    
    /*
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Config)) return false;
        Config other = (Config) obj;
        if (!key().equals(other.key())) return false;
        if (hasBody() != other.hasBody()) return false;
        if (hasDocument() != other.hasDocument()) return false;
        if (hasValue() != other.hasValue()) return false;
        if (hasValue() && !value().equals(other.value())) return false;
        if (hasDocument() && (
                !documentType().equals(other.documentType()) ||
                !Arrays.equals(documentContent(), other.documentContent()))) return false;
        if (hasBody() && !body().equals(other.body())) return false;
        return true; // phew
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(key(), hasBody(), hasDocument(), hasValue(),
                body(), documentType(), documentContent(), value());
    }
    */

}
