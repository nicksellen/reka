package reka.filesystem;

import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class FilesystemMktempDir implements Operation {

	private final java.nio.file.Path tmpdirbase;
	private final Path dirname;
	
	public FilesystemMktempDir(java.nio.file.Path tmpdir, Path dirname) {
		this.tmpdirbase = tmpdir;
		this.dirname = dirname;
	}

	@Override
	public void call(MutableData data) {
		try {
			java.nio.file.Path tmp = Files.createTempDirectory(tmpdirbase, ".rekatmp");
			data.putString(dirname, tmp.toAbsolutePath().toString());
			tmp.toFile().deleteOnExit();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}
