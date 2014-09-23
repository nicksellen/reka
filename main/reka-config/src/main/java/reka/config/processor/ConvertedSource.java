package reka.config.processor;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import reka.config.AbstractSource;
import reka.config.Source;

public class ConvertedSource extends AbstractSource {

	private final ConfigConverter converter;
	private final Source parent;
	
	public ConvertedSource(ConfigConverter converter, Source parent) {
		checkNotNull(parent);
		this.converter = converter;
		this.parent = parent;
	}
	
	@Override
	public String content() {
		return parent.content();
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public File file() {
		return null;
	}

	@Override
	public boolean hasParent() {
		return true;
	}

	@Override
	public Source parent() {
		return parent;
	}

	@Override
	public Source origin() {
		return parent.origin();
	}
	
	@Override
	public String toString() {
		return format("%s('%s' %s)", getClass().getSimpleName(), converter.getClass().getSimpleName(), parent);
	}

    @Override
    public String location() {
        return origin().location();
    }

    @Override
    public boolean supportsNestedFile() {
        return parent.supportsNestedFile();
    }

    @Override
    public Path nestedFile(String location) {
    	return parent.nestedFile(location);
    }
    
    @Override
    public List<Path> nestedFiles(String location) {
    	return parent.nestedFiles(location);
    }

}
