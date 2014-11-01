package reka.api.content;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import reka.api.content.types.BinaryContent;
import reka.api.content.types.BinaryContent.Encoding;
import reka.api.content.types.BooleanContent;
import reka.api.content.types.ByteArrayBinaryContent;
import reka.api.content.types.ByteBufferBinaryContent;
import reka.api.content.types.DoubleContent;
import reka.api.content.types.FileBinaryContent;
import reka.api.content.types.IntegerContent;
import reka.api.content.types.LongContent;
import reka.api.content.types.NullContent;
import reka.api.content.types.UTF8Content;

public class Contents {
	
	public static Content doubleValue(double value) {
		return new DoubleContent(value);
	}
	
	public static Content booleanValue(boolean value) {
		return BooleanContent.of(value);
	}
	
	public static Content nullValue() {
		return NullContent.INSTANCE;
	}
	
	public static Content utf8(String value) {
		return new UTF8Content(value);
	}

	public static Content integer(int value) {
		return new IntegerContent(value);
	}

	public static Content longValue(long value) {
		return new LongContent(value);
	}
	
	public static Content binary(String contentType, InputStream stream) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b;
			while ((b = stream.read()) != -1) {
				baos.write(b);
			}
			return new ByteArrayBinaryContent(contentType, Encoding.NONE, baos.toByteArray());
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public static Content binary(String contentType, ByteBuffer buffer) {
		return new ByteBufferBinaryContent(contentType, BinaryContent.Encoding.NONE, buffer);
	}

	public static Content binary(String contentType, File file) {
		return new FileBinaryContent(contentType, BinaryContent.Encoding.NONE, file);
	}

	public static Content binary(String contentType, byte[] bytes) {
		return new ByteArrayBinaryContent(contentType, BinaryContent.Encoding.NONE, bytes);
	}

	public static Content binary(String contentType, byte[] bytes, String encoding) {
		return new ByteArrayBinaryContent(contentType, BinaryContent.Encoding.valueOf(encoding.toUpperCase()), bytes);
	}
	
	public static byte[] writeValueToByteArray(Content content) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			content.out(new DataOutputStream(baos));
			return baos.toByteArray();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public static Content in(DataInput in) {
		try {
			return Content.Type.fromIdentifier(in.readByte()).in(in);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

}