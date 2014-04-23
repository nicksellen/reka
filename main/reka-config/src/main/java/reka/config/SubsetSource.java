package reka.config;

import static java.lang.String.format;

import java.io.File;

public class SubsetSource extends AbstractSource {
	
	private final Source parent;
	private final int start;
	private final int length;
	
	public SubsetSource(Source parent, int start, int length) {
		this.parent = parent;
		this.start = start;
		this.length = length;
	}

	@Override
	public String content() {
		return parent.content().substring(start, start + length);
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
	public Source origin() {
		return parent().origin();
	}

	@Override
	public Source parent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return format("%s(%s[%s-%s])", getClass().getSimpleName(), parent, start, start + length);
	}

	@Override
	public int originOffsetStart() {
		return start;
	}

	@Override
	public int originOffsetLength() {
		return length;
	}

    @Override
    public boolean supportsNestedData() {
        return parent.supportsNestedData();
    }

    @Override
    public byte[] nestedData(String location) {
    	return parent.nestedData(location);
    }

    @Override
    public boolean supportsNestedFile() {
        return parent.supportsNestedData();
    }

    @Override
    public File nestedFile(String location) {
    	return parent.nestedFile(location);
    }

    @Override
    public String location() { 
        return format("%s:%s-%s", origin().location(), start, start + length);
    }

}
