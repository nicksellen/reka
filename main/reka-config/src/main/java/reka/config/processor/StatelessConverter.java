package reka.config.processor;

public abstract class StatelessConverter implements ConfigConverter {
    
    @Override
    public ConfigConverter resetOrClone() {
        return this;
    }

}
