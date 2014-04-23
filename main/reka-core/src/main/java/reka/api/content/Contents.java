package reka.api.content;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import reka.api.content.Content.BinaryContent;
import reka.api.content.Content.BinaryContent.Encoding;
import reka.api.content.Content.ByteArrayBinaryContent;
import reka.api.content.Content.ByteBufferBinaryContent;
import reka.api.content.Content.DoubleContent;
import reka.api.content.Content.FalseContent;
import reka.api.content.Content.FileBinaryContent;
import reka.api.content.Content.IntegerContent;
import reka.api.content.Content.LongContent;
import reka.api.content.Content.NonSerializeableObject;
import reka.api.content.Content.NullContent;
import reka.api.content.Content.TrueContent;
import reka.api.content.Content.UTF8Content;

public class Contents {
	
	public static Content doubleValue(double value) {
		return new DoubleContent(value);
	}
	
	public static Content falseValue() {
		return FalseContent.INSTANCE;
	}
	
	public static Content trueValue() {
		return TrueContent.INSTANCE;
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

	/*
	public static Content binary(ByteBuffer buffer) {
		return new ByteBufferBinaryContent(null, BinaryContent.Encoding.NONE, buffer);
	}


	public static Content binary(File file) {
		return new FileBinaryContent(null, BinaryContent.Encoding.NONE, file);
	}

	public static Content binary(byte[] bytes) {
		return new ByteArrayBinaryContent(null, BinaryContent.Encoding.NONE, bytes);
	}
	*/
	
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
	
    public static Content nonSerializableContent(Object object) {
        return new NonSerializeableObject(object);
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