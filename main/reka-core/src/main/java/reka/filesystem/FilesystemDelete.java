package reka.filesystem;

import static reka.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class FilesystemDelete implements SyncOperation {
	
	private final java.nio.file.Path basedir;
	private final Function<Data,String> filenameFn;
	
	public FilesystemDelete(java.nio.file.Path basedir, Function<Data,String> filenameFn) {
		this.basedir = basedir;
		this.filenameFn = filenameFn;	
	}
	
	@Override
	public MutableData call(MutableData data) {
		
		String filename = filenameFn.apply(data);
		try {
			Files.delete(resolveAndCheck(basedir, filename));
		} catch (IOException e) {
			throw unchecked(e);
		}
		
		return data;
	}

}
