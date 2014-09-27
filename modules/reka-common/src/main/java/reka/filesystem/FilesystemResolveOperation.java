package reka.filesystem;

import static reka.api.Path.dots;
import static reka.filesystem.FilesystemUtils.resolveAndCheck;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class FilesystemResolveOperation implements Operation {
	
	private final java.nio.file.Path basedir;
	private final Function<Data,String> inFn, outFn;
	
	public FilesystemResolveOperation(java.nio.file.Path basedir, Function<Data,String> inFn, Function<Data,String> outFn) {
		this.basedir = basedir;
		this.inFn = inFn;
		this.outFn = outFn;
	}

	@Override
	public void call(MutableData data) {
		data.putString(dots(outFn.apply(data)), resolveAndCheck(basedir, inFn.apply(data)).normalize().toString());
	}

}
