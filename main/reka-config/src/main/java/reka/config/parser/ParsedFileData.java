package reka.config.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

class ParsedFileData implements ParsedData {

	private final String location;
	private final String type;
	private final File file;
	
	public ParsedFileData(String location, String type, File file) {
		this.location = location;
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
	public String location() {
		return location;
	}
	
	@Override
	public String toString() {
		return String.format("%s('%s')", getClass().getSimpleName(), file.toPath().toAbsolutePath().toString());
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
