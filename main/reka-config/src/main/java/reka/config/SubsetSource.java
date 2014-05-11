package reka.config;

import static java.lang.String.format;
import static reka.config.SourceUtils.fromend;
import static reka.config.SourceUtils.occurances;

import java.io.File;

public class SubsetSource extends AbstractSource {
	
	private final Source parent;
	private final int offset;
	private final int length;
	
	public SubsetSource(Source parent, int offset, int length) {
		this.parent = parent;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public String content() {
		return parent.content().substring(offset, offset + length);
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
		return format("%s(%s)", getClass().getSimpleName(), location());
	}

	@Override
	public int originOffsetStart() {
		return offset;
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
    	return origin().location();
    }

	@Override
	public SourceLinenumbers linenumbers() {
		
		String before = parent.content().substring(0, offset);
		int lineStart = occurances(before, '\n') + 1;
		int posStart = fromend(before, '\n') + 1;

    	String content = parent.content().substring(offset, offset + length);
    	
    	int lineEnd = lineStart + occurances(content, '\n');
    	int posEnd = fromend(content, '\n');
    	
    	if (lineStart == lineEnd) {
    		posEnd += posStart;
    	}
		
		return new SourceLinenumbers(lineStart, posStart, lineEnd, posEnd);
	}

}
