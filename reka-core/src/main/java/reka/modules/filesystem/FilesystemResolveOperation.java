package reka.modules.filesystem;

import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Path.dots;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class FilesystemResolveOperation implements Operation {
	
	private final java.nio.file.Path basedir;
	private final Function<Data,String> inFn, outFn;
	
	public FilesystemResolveOperation(java.nio.file.Path basedir, Function<Data,String> inFn, Function<Data,String> outFn) {
		this.basedir = basedir;
		this.inFn = inFn;
		this.outFn = outFn;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.putString(dots(outFn.apply(data)), resolveAndCheck(basedir, inFn.apply(data)).normalize().toString());
	}

}
