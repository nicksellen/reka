package reka.api.content.types;

import java.nio.ByteBuffer;

public class ByteBufferBinaryContent extends BinaryContent {

	private final ByteBuffer buffer;
	
	public ByteBufferBinaryContent(String contentType, Encoding encoding, ByteBuffer buffer) {
		super(contentType, encoding);
		this.buffer = buffer;
	}
	
	@Override
	public boolean hasByteBuffer() {
		return true;
	}
	
	@Override
	public ByteBuffer asByteBuffer() {
		return buffer;
	}

	@Override
	protected byte[] bytes() {
		buffer.mark();
		try {
			byte[] result = new byte[buffer.remaining()];
			buffer.get(result);
			return result;
		} finally {
			buffer.reset();
		}
	}

	@Override
	protected long size() {
		return buffer.remaining();
	}
	
	@Override
	public Object value() {
		return buffer;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buffer == null) ? 0 : buffer.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof ByteBufferBinaryContent) {
			ByteBufferBinaryContent other = (ByteBufferBinaryContent) obj;
			if (buffer == null) {
				if (other.buffer != null)
					return false;
			} else if (!buffer.equals(other.buffer))
				return false;
			return true;
		} else if (obj instanceof BinaryContent) {
			return BinaryContent.equals(this, (BinaryContent) obj);
		} else {
			return false;
		}
	}
	
}