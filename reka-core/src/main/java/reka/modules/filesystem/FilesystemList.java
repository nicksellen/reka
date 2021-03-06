package reka.modules.filesystem;

import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;

import java.io.File;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;

public class FilesystemList implements Operation {
	
	public static final String DIRECTORY = "directory";
	public static final String FILE = "file";
	public static final String NOT_FOUND = "not found";
	
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger("filesystem/list");

	private final java.nio.file.Path basedir;
	
	private final Function<Data,Path> dataPathFn;
	private final Function<Data,String> dirFn;

	public FilesystemList(java.nio.file.Path basedir, Function<Data,Path> dataPathFn, Function<Data,String> dirFn) {
		this.basedir = basedir;
		this.dataPathFn = dataPathFn;
		this.dirFn = dirFn;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {

		Path dataOut = dataPathFn.apply(data);
		
		String dir = dirFn.apply(data);
		
		java.nio.file.Path entryPath = resolveAndCheck(basedir, dir);
		File entry = entryPath.toFile();
		
		if (entry.isDirectory()) {
			
			File[] files = entry.listFiles();
			
			if (files != null && files.length > 0) {
			
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					String path = dir.isEmpty() ? file.getName() : entryPath.resolve(file.getName()).toString();
					populate(data, dataOut.add(i), file, path);
				}
			
			} else {
				data.createListAt(dataOut);
			}
		} else {
			populate(data, dataOut.add(0), entry, dir);
		}
	}
	
	private void populate(MutableData item, Path base, File file, String path) {
		item.putString(base.add("name"), file.getName());
		item.putString(base.add("path"), path);
		item.putLong(base.add("size"), file.length());
		item.putLong(base.add("last-modified"), file.lastModified());
		item.putBool(base.add("dir?"), file.isDirectory());
		item.putBool(base.add("hidden?"), file.isHidden());
	}

}
