package reka.config.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ParsedFileDocument implements ParsedDocument {
	
	private final String type;
	private final File file;
	
	public ParsedFileDocument(String type, File file) {
		this.type = type;
		this.file = file;
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public byte[] content() {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public File file() {
		return file;
	}

}
