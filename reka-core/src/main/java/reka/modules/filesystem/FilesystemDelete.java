package reka.modules.filesystem;

import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Util.deleteRecursively;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class FilesystemDelete implements Operation {
	
	private final java.nio.file.Path basedir;
	private final Function<Data,String> filenameFn;
	
	public FilesystemDelete(java.nio.file.Path basedir, Function<Data,String> filenameFn) {
		this.basedir = basedir;
		this.filenameFn = filenameFn;	
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		deleteRecursively(resolveAndCheck(basedir, filenameFn.apply(data)));
	}

}
