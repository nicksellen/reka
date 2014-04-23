package reka.config.parser;

import java.io.File;

public class ByteArrayConfigData implements ParsedData {

    private final String type;
    private final byte[] content;
    
    public ByteArrayConfigData(String type, byte[] content) {
        this.type = type;
        this.content = content;
    }
    
    @Override
    public String type() {
        return type;
    }

    @Override
    public byte[] content() {
        return content;
    }

    @Override
    public String location() {
        return null;
    }

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public File file() {
		throw new UnsupportedOperationException();
	}

}
