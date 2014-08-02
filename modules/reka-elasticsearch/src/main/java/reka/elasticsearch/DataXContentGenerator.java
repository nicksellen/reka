package reka.elasticsearch;

import static reka.api.Path.root;
import static reka.api.content.Contents.doubleValue;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.nullValue;
import static reka.api.content.Contents.trueValue;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unsupported;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentString;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.support.AbstractXContentGenerator;

import reka.api.Path;
import reka.api.data.MutableData;

public class DataXContentGenerator extends AbstractXContentGenerator {
	
	private final MutableData data;
	
	private Path field;
	private Deque<MutableData> stack = new ArrayDeque<>();
	private MutableData current;
	
	public DataXContentGenerator(MutableData data) {
		this.data = data;
		current = data;
		stack.push(current);
	}
	
	public MutableData data() {
		return data;
	}

	@Override
	public XContentType contentType() {
		return null;
	}

	@Override
	public void usePrettyPrint() { }

	@Override
	public void usePrintLineFeedAtEnd() { }

	@Override
	public void writeStartArray() throws IOException {
		if (field != null) {
			current = current.createMapAt(field);
		} else {
			current.putList(root(), (unused) -> {});
			current = current.mutableAt(root());
		}
		stack.push(current);
		field = null;
	}

	@Override
	public void writeEndArray() throws IOException {
		stack.pop();
		current = stack.peek();
		field = null;
	}

	@Override
	public void writeStartObject() throws IOException {
		if (field != null) {
			current = current.createMapAt(field);
		} else {
			current = current.createMapAt(root());
		}
		stack.push(current);
		field = null;
	}

	@Override
	public void writeEndObject() throws IOException {
		stack.pop();
		current = stack.peek();
		field = null;
	}

	@Override
	public void writeFieldName(String name) throws IOException {
		field = Path.path(name);
	}

	@Override
	public void writeString(String text) throws IOException {
		current.put(field, utf8(text));
	}

	@Override
	public void writeNumber(int v) throws IOException {
		current.put(field, integer(v));
	}

	@Override
	public void writeNumber(long v) throws IOException {
		current.put(field, longValue(v));
	}

	@Override
	public void writeNumber(double d) throws IOException {
		current.put(field, doubleValue(d));
	}

	@Override
	public void writeNumber(float f) throws IOException {
		current.put(field, doubleValue(f));
	}

	@Override
	public void writeBoolean(boolean state) throws IOException {
		current.put(field, state ? trueValue() : falseValue());
	}

	@Override
	public void writeNull() throws IOException {
		current.put(field, nullValue());
	}

	@Override
	public void writeArrayFieldStart(XContentString fieldName) throws IOException {
		writeFieldName(fieldName);
		writeStartArray();
	}

	@Override
	public void writeObjectFieldStart(XContentString fieldName) throws IOException {
		writeFieldName(fieldName);
		writeStartObject();
	}

	@Override
	public void writeFieldName(XContentString name) throws IOException {
		writeFieldName(name.getValue());
	}

	@Override
	public void writeString(char[] text, int offset, int len) throws IOException {
		writeString(new String(text, offset, len));
	}

	@Override
	public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
		writeString(new String(text, offset, length));
	}

	@Override
	public void writeBinary(byte[] data, int offset, int len) throws IOException {
		throw unsupported();
	}

	@Override
	public void writeBinary(byte[] data) throws IOException {
		throw unsupported();
	}

	@Override
	public void writeStringField(XContentString fieldName, String value) throws IOException {
		writeFieldName(fieldName);
		writeString(value);
	}

	@Override
	public void writeBooleanField(XContentString fieldName, boolean value) throws IOException {
		writeFieldName(fieldName);
		writeBoolean(value);
	}

	@Override
	public void writeNullField(XContentString fieldName) throws IOException {
		writeFieldName(fieldName);
		writeNull();
	}

	@Override
	public void writeNumberField(XContentString fieldName, int value)
			throws IOException {
		writeFieldName(fieldName);
		writeNumber(value);
	}

	@Override
	public void writeNumberField(XContentString fieldName, long value) throws IOException {
		writeFieldName(fieldName);
		writeNumber(value);
	}

	@Override
	public void writeNumberField(XContentString fieldName, double value) throws IOException {
		writeFieldName(fieldName);
		writeNumber(value);
	}

	@Override
	public void writeNumberField(XContentString fieldName, float value) throws IOException {
		writeFieldName(fieldName);
		writeNumber(value);
	}

	@Override
	public void writeBinaryField(XContentString fieldName, byte[] data) throws IOException {
		throw unsupported();
	}

	@Override
	public void writeRawField(String fieldName, byte[] content, OutputStream bos) throws IOException {
		throw unsupported();
	}

	@Override
	public void writeRawField(String fieldName, byte[] content, int offset, int length, OutputStream bos) throws IOException {
		throw unsupported();
	}

	@Override
	public void writeRawField(String fieldName, InputStream content, OutputStream bos) throws IOException {
		throw unsupported();
	}

	@Override
	public void writeRawField(String fieldName, BytesReference content, OutputStream bos) throws IOException {
		throw unsupported();
	}

	@Override
	public void copyCurrentStructure(XContentParser parser) throws IOException {
		XContentHelper.copyCurrentStructure(this, parser);
	}

	@Override
	public void flush() throws IOException { }

	@Override
	public void close() throws IOException { }

}
