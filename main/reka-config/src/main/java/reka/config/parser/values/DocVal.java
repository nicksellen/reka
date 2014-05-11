package reka.config.parser.values;

public class DocVal extends ByteVal {
	
	private final String contentType;

	public DocVal(String contentType, byte[] value) {
		super(value);
		this.contentType = contentType;
	}
	
	public String contentType() {
		return contentType;
	}

}