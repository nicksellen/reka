package reka.data.content.types;

import java.util.Arrays;


public class ByteArrayBinaryContent extends BinaryContent {

	private final byte[] bytes;
	
	public ByteArrayBinaryContent(String contentType, Encoding encoding, byte[] bytes) {
		super(contentType, encoding);
		this.bytes = bytes;
	}

	@Override
	protected byte[] bytes()  {
		return bytes;
	}

	@Override
	protected long size() {
		return bytes.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof ByteArrayBinaryContent) {
			return Arrays.equals(bytes, ((ByteArrayBinaryContent) obj).bytes);
		} else if (obj instanceof BinaryContent) {
			return BinaryContent.equals(this, (BinaryContent) obj);
		} else {
			return false;
		}
	}
	
}