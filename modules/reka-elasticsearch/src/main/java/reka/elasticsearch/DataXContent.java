package reka.elasticsearch;

import static reka.util.Util.unsupported;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentGenerator;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

import reka.api.data.MutableData;

public class DataXContent implements XContent {

	private final MutableData data;
	
	public DataXContent(MutableData data) {
		this.data = data;
	}
	
	@Override
	public XContentType type() {
		return null;
	}

	@Override
	public byte streamSeparator() {
		throw unsupported();
	}

	@Override
	public XContentGenerator createGenerator(OutputStream os) throws IOException {
		return new DataXContentGenerator(data);
	}

	@Override
	public XContentGenerator createGenerator(Writer writer) throws IOException {
		return new DataXContentGenerator(data);
	}

	@Override
	public XContentParser createParser(String content) throws IOException {
		throw unsupported();
	}

	@Override
	public XContentParser createParser(InputStream is) throws IOException {
		throw unsupported();
	}

	@Override
	public XContentParser createParser(byte[] data) throws IOException {
		throw unsupported();
	}

	@Override
	public XContentParser createParser(byte[] data, int offset, int length)
			throws IOException {
		throw unsupported();
	}

	@Override
	public XContentParser createParser(BytesReference bytes) throws IOException {
		throw unsupported();
	}

	@Override
	public XContentParser createParser(Reader reader) throws IOException {
		throw unsupported();
	}

}
