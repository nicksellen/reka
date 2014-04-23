package reka.config;

import static com.google.common.base.Preconditions.checkState;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.NoSuchElementException;

import reka.config.formatters.ConfigFormatter;
import reka.config.formatters.Formatter;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;

public abstract class AbstractConfig implements Config {
    
    private final Source source;
    
    protected AbstractConfig(Source source) {
        this.source = source;
    }
    
    @Override
    public boolean hasKey() {
    	return key() != null;
    }
    
    @Override
    public Source source() {
        return source;
    }

    @Override
    public boolean hasBody() {
        return false;
    }
    
    @Override
    public ConfigBody body() {
        return null;
    }

    @Override
    public boolean hasData() {
        return false;
    }
    
    @Override
    public String dataType() {
        return null;
    }
    
    @Override
    public byte[] data() {
        return null;
    }
    
    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public Object value() {
        return null;
    }
    
    @Override
    public BigDecimal valueAsBigDecimal() {
        throw new NoSuchElementException();
    }
    
    @Override
    public String valueAsString() {
        throw new NoSuchElementException();
    }

    @Override
    public Number valueAsNumber() {
        throw new NoSuchElementException();
    }

    @Override
    public int valueAsInt() {
        throw new NoSuchElementException();
    }

    @Override
    public long valueAsLong() {
        throw new NoSuchElementException();
    }

    @Override
    public double valueAsDouble() {
        throw new NoSuchElementException();
    }

    @Override
    public boolean hasDocument() {
        return false;
    }

    @Override
    public String documentType() {
        return null;
    }

    @Override
    public byte[] documentContent() {
        return null;
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

}
