package reka.config;

import java.util.Arrays;
import java.util.Iterator;

import reka.config.formatters.Formattable;

public abstract class ConfigBody implements NavigableConfig, Iterable<Config>, Formattable {
    
    public static ConfigBody of(Source source, Iterable<Config> incoming) {
        return BaseConfigBody.of(source, incoming);
    }
    
    public static ConfigBody of(Source source, Config... incoming) {
        return of(source, Arrays.asList(incoming));
    }
    
    @Override
    public boolean equals(Object obj) {
        // TODO: should I add in 'source' into this?
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof ConfigBody)) return false;
        ConfigBody other = (ConfigBody) obj;
        Iterator<Config> a = this.iterator();
        Iterator<Config> b = other.iterator();
        while (a.hasNext() && b.hasNext()) {
            if (!a.next().equals(b.next())) {
                return false;
            }
        }
        return !a.hasNext() && !b.hasNext();
    }
    
    @Override
    public int hashCode() {
        // based on Arrays.hashCode
        int result = 1;
        for (Config element : this)
            result = 31 * result + (element == null ? 0 : element.hashCode());
        return result;
    }
    
}
