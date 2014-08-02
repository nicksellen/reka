package reka.gitfordata.tree;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;

import com.google.common.io.BaseEncoding;

public class ObjectId {
	
	public static final int SIZE = 20;
	
	private static final BaseEncoding HEX_ENCODER = BaseEncoding.base16();
	private static final BaseEncoding BASE64_ENCODER = BaseEncoding.base64();

	public static ObjectId fromBytes(byte[] value) {
		return new ObjectId(value);
	}
	
	public static ObjectId fromHex(String hex) {
		return fromBytes(HEX_ENCODER.decode(hex.toUpperCase()));
	}
	
	public static ObjectId fromBase64(String base64) {
		return fromBytes(BASE64_ENCODER.decode(base64));
	}
	
	private final byte[] bytes;
	private final int hashCode;
	
	private ObjectId(byte[] value) {
		checkArgument(value.length == SIZE, "ObjectId must be %s long (not %s)", SIZE, value.length);
		bytes = value;
		hashCode = Arrays.hashCode(bytes);
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other instanceof ObjectId) {
			return Arrays.equals(bytes, ((ObjectId) other).bytes);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
	
	public byte[] bytes() {
		return bytes;
	}
	
	public String hex() {
		return HEX_ENCODER.encode(bytes).toLowerCase();
	}
	
	public String base64() {
		return BASE64_ENCODER.encode(bytes);
	}
	
	@Override
	public String toString() {
		return hex();
	}
	
}
