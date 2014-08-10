package reka.config;

import java.util.Optional;

import reka.config.formatters.Formattable;

public interface NavigableConfig extends Formattable, Iterable<Config> {
    
    public Source source(); // TODO this doesn't quite belong here
    
    public int elementCount();
    public Optional<Config> at(String path);
    public Iterable<Config> each();
    public Iterable<Config> each(String path);
    public Iterable<Config> eachChildOf(String path);
    
}
