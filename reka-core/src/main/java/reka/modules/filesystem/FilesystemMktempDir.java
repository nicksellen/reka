package reka.modules.filesystem;

import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;

import reka.api.Path;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class FilesystemMktempDir implements Operation {

	private final java.nio.file.Path tmpdirbase;
	private final Path dirname;
	
	public FilesystemMktempDir(java.nio.file.Path tmpdir, Path dirname) {
		this.tmpdirbase = tmpdir;
		this.dirname = dirname;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		try {
			Files.createDirectories(tmpdirbase);
			java.nio.file.Path tmp = Files.createTempDirectory(tmpdirbase, "fs.tmp.");
			data.putString(dirname, tmpdirbase.relativize(tmp).toString());
			tmp.toFile().deleteOnExit();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}
