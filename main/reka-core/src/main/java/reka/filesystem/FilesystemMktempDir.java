package reka.filesystem;

import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class FilesystemMktempDir implements SyncOperation {

	private final Path dirname;
	
	public FilesystemMktempDir(Path dirname) {
		this.dirname = dirname;
	}

	@Override
	public MutableData call(MutableData data) {
		try {
			java.nio.file.Path tmp = Files.createTempDirectory(".rekatmp");
			data.putString(dirname, tmp.toAbsolutePath().toString());
			tmp.toFile().deleteOnExit();
			return data;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}
