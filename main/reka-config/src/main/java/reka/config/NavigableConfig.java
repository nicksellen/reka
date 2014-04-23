package reka.config;

import reka.config.formatters.Formattable;

import com.google.common.base.Optional;

public interface NavigableConfig extends Formattable, Iterable<Config> {
    
    public Source source(); // TODO this doesn't quite belong here
    
    public int elementCount();
    public Optional<Config> at(String path);
    public Iterable<Config> each();
    public Iterable<Config> each(String path);
    public Iterable<Config> eachChildOf(String path);
    
}
