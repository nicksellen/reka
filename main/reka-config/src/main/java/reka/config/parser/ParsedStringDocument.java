package reka.config.parser;

import java.io.File;

import com.google.common.base.Charsets;

class ParsedStringDocument implements ParsedDocument {

	private final String type;
	private final String content;
	
	public static ParsedDocument create(String type, String content) {
		return new ParsedStringDocument(type, content);
	}
	
	private ParsedStringDocument(String type, String content) {
		this.type = type;
		this.content = content;
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public byte[] content() {
		return content.getBytes(Charsets.UTF_8);
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
