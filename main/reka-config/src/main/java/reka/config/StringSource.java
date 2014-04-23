package reka.config;

import java.io.File;

public class StringSource extends AbstractSource {
    
    private final String content;
    private final Source parent;
    
    public static Source from(String content) {
    	return from(content, null);
    }
    
    public static Source from(String content, Source parent) {
        return new StringSource(content, parent);
    }
    
    private StringSource(String content, Source parent) {
        this.content = content;
        this.parent = parent;
    }

    @Override
    public String content() {
        return content;
    }
    
    @Override
    public Source origin() {
    	return this;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public File file() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasParent() {
        return parent != null;
    }

    @Override
    public Source parent() {
    	return parent;
    }

    @Override
    public String location() {
        return "<string>";
    }

}
