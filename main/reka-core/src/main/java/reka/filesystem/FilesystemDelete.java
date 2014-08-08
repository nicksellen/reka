package reka.filesystem;

import static reka.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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
			Files.walkFileTree(resolveAndCheck(basedir, filename), new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					if (exc != null) throw exc;
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (exc != null) throw exc;
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				
			});
		} catch (IOException e) {
			throw unchecked(e);
		}
		
		return data;
	}

}
