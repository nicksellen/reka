package reka.filesystem;

import static reka.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Util.deleteRecursively;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class FilesystemDelete implements Operation {
	
	private final java.nio.file.Path basedir;
	private final Function<Data,String> filenameFn;
	
	public FilesystemDelete(java.nio.file.Path basedir, Function<Data,String> filenameFn) {
		this.basedir = basedir;
		this.filenameFn = filenameFn;	
	}
	
	@Override
	public void call(MutableData data) {
		deleteRecursively(resolveAndCheck(basedir, filenameFn.apply(data)));
	}

}
