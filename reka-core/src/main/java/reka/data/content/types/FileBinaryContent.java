package reka.data.content.types;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import reka.util.Util;

public class FileBinaryContent extends BinaryContent {

	private final File file;
	
	public FileBinaryContent(String contentType, Encoding encoding, File file) {
		super(contentType, encoding);
		this.file = file;
	}
	
	@Override
	public boolean hasFile() {
		return true;
	}
	
	@Override
	public File asFile() {
		return file;
	}

	@Override
	protected byte[] bytes()  {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			throw Util.unchecked(e);
		}
	}

	@Override
	protected long size() {
		return file.length();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof FileBinaryContent) {
			FileBinaryContent other = (FileBinaryContent) obj;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			return true;
		} else if (obj instanceof BinaryContent) {
			return BinaryContent.equals(this, (BinaryContent) obj);
		} else {
			return false;
		}
	}
	
}