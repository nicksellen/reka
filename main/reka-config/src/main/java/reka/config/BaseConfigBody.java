package reka.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import reka.config.formatters.ConfigFormatter;
import reka.config.formatters.Formatter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class BaseConfigBody extends ConfigBody {
    
    private final Source source;
    private final List<Config> configs;
    
    public static BaseConfigBody of(Source source, Iterable<Config> incoming) {
        return new BaseConfigBody(source, incoming);
    }
    
    private BaseConfigBody(Source source, Iterable<Config> incoming) {
    	checkNotNull(source);
        this.source = source;
        this.configs = ImmutableList.copyOf(incoming);
    }
    
    public int elementCount() {
        return configs.size();
    }

    @Override
    public Iterator<Config> iterator() {
        return configs.iterator();
    }
    
    @Override
    public String toString() {
        return format();
    }

    @Override
    public <T> T format(Formatter<T> out, FormattingOptions opts) {
        for (Config config : configs) {
            config.format(out, opts);
        }
        return out.format();
    }

    @Override
    public String format(FormattingOptions opts) {
        return format(new ConfigFormatter(), opts);
    }

    @Override
    public String format() {
        return format(new ConfigFormatter(), new FormattingOptions());
    }

    @Override
    public <T> T format(Formatter<T> out) {
        return format(out, new FormattingOptions());
    }
    
    @Override
    public Iterable<Config> each() {
        return configs;
    }

    @Override
    public Optional<Config> at(String path) {
        ConfigBody current = this;
        String[] elements = path.split("\\.");
        for (int i = 0; i < elements.length - 1; i++) {
            String e = elements[i];
            boolean found = false;
            for (Config item : current) {
                if (!item.hasBody()) continue;
                if (item.key().equals(e)) {
                    current = item.body();
                    found = true;
                    continue;
                }
            }
            if (!found) return Optional.absent();
        }
        String last = elements[elements.length - 1];
        for (Config item : current) {
            if (last.equals(item.key())) {
                return Optional.of(item);
            }
        }
        return Optional.absent();
    }
    
    @Override
    public Iterable<Config> each(String path) {
        return walk(this, path.split("\\."), new LinkedList<Config>());
    }
    
    private List<Config> walk(ConfigBody body, String[] path, List<Config> results) {
        boolean end = path.length == 1;
        for (Config config : body) {
            if (config.key().equals(path[0])) {
                if (end) {
                    results.add(config);
                } else if (config.hasBody()){
                    walk(config.body(), removeFirst(path), results);
                }
            }
        }
        return results;
    }
    
    private String[] removeFirst(String[] path) {
        String[] next = new String[path.length - 1];
        System.arraycopy(path, 1, next, 0, next.length);
        return next;
    }

    @Override
    public Iterable<Config> eachChildOf(String path) {
        List<Config> results = new LinkedList<>();
        for (Config child : each(path)) {
            if (child.hasBody()) {
                for (Config childOfChild : child.body()) {
                    results.add(childOfChild);
                }
            }
        }
        return results;
    }

    @Override
    public Source source() {
        return source;
    }
    
}
