package reka.api.content.types;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.codehaus.jackson.JsonGenerator;

import reka.api.content.Content;
import reka.core.data.ObjBuilder;
import reka.util.Util;

import com.google.common.hash.Hasher;

public abstract class BinaryContent implements Content {
	
	public static enum Encoding { NONE, BASE64 };
	
	public static final String JSON_TYPE = "binary/1.0";
	
	protected final String contentType;
	protected final BinaryContent.Encoding encoding; // what kind of bytes do we have?
	
	protected BinaryContent(String contentType, BinaryContent.Encoding encoding) {
		if (contentType == null) {
			Util.printStackTrace();
		}
		checkNotNull(contentType, "must include content type");
		this.contentType = contentType;
		this.encoding = encoding;
	}
	
	@Override
	public void writeObj(ObjBuilder obj) {
		if (contentType != null && (contentType.startsWith("text/") || contentType.startsWith("application/"))) {
			obj.writeValue(new String(as(Encoding.NONE).bytes(), StandardCharsets.UTF_8));
		} else {
			obj.writeValue(as(Encoding.NONE).bytes()); // ? this might not really want a byte array...
		}
	}
	
	@Override
	public Object value() {
		if (contentType != null && (contentType.startsWith("text/") || contentType.startsWith("application/"))) {
			return new String(as(Encoding.NONE).bytes(), StandardCharsets.UTF_8);
		} else {
			return as(Encoding.NONE).bytes();
		}
	}

	@Override
	public Type type() {
		return Type.BINARY;
	}

	@Override
	public void writeJsonTo(JsonGenerator json) throws IOException {
		BINARY_CONVERTER.out(this, json);
	}

	@Override
	public void out(DataOutput out) throws IOException {
		BINARY_CONVERTER.out(this, out);
	}

	@Override
	public Hasher hash(Hasher hasher) {
		if (contentType != null) {
			hasher.putString(contentType, StandardCharsets.UTF_8);
		}
		return hasher.putLong(size()).putBytes(bytes());
	}
	
	@Override
	public byte[] asBytes() {
		return bytes();
	}
	
	public String asDataURI() {
		checkState(contentType != null, "can't make a DataURI without the content/type");
		return format("data:%s;base64,%s", contentType, base64String());
	}
	
	public BinaryContent as(BinaryContent.Encoding requiredEncoding) {
		if (requiredEncoding == encoding) return this;
		return new ByteArrayBinaryContent(contentType, requiredEncoding, encoded(requiredEncoding));
	}
	
	private byte[] encoded(BinaryContent.Encoding requiredEncoding) {
		if (requiredEncoding == encoding) {
			return bytes();
		} else if (encoding == Encoding.BASE64 && requiredEncoding == Encoding.NONE) {
			return BASE64_DECODER.decode(bytes());
		} else if (encoding == Encoding.NONE && requiredEncoding == Encoding.BASE64) {
			return base64String().getBytes(StandardCharsets.UTF_8);
		} else {
			throw Util.runtime("don't know how to turn [%s] -> [%s]", encoding, requiredEncoding);
		}
	}
	
	public String base64String() {
		if (encoding == Encoding.BASE64) {
			return new String(bytes(), StandardCharsets.UTF_8);
		} else {
			return new String(BASE64_ENCODER.encode(bytes()), StandardCharsets.UTF_8);
		}
	}
	
	public byte[] base64() {
		return encoded(Encoding.BASE64);
	}
	
	public byte[] decoded() {
		return encoded(Encoding.NONE);
	}
	
	public BinaryContent.Encoding encoding() {
		return encoding;
	}
	
	public String contentType() {
		return contentType;
	}
	
	protected abstract byte[] bytes();
	protected abstract long size();

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public String toString() {
		if (contentType != null) {
			return format("BinaryContent(%s, %d bytes)", contentType, size());
		} else {
			return format("BinaryContent(%d bytes)", size());
		}
	}
	
	@Override
	public String asUTF8() {
		return new String(bytes(), StandardCharsets.UTF_8);
	}

	public static boolean equals(BinaryContent a, BinaryContent b) {
		if (a.size() != b.size()) return false;
		return Arrays.equals(a.bytes(), b.bytes());
	}
	
}