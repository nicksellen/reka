package reka.config;

import java.io.File;

public abstract class AbstractSource implements Source {
    
	@Override
	public int originOffsetStart() {
		return 0;
	}

	@Override
	public int originOffsetLength() {
		return content().length();
	}
	
	@Override
	public Source rootOrigin() {
	    Source origin = origin();
	    while (origin.parent() != null) {
	        origin = origin.parent().origin();
	    }
	    return origin;
	}

	@Override
	public boolean supportsNestedFile() {
		return false;
	}

	@Override
	public File nestedFile(String location) {
		return null;
	}

	@Override
	public boolean supportsNestedData() {
		return false;
	}

	@Override
	public byte[] nestedData(String location) {
		return null;
	}


}
