package reka.filesystem;

import static reka.filesystem.FilesystemUtils.resolveAndCheck;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;

public class FilesystemType implements RoutingOperation {
	
	private final Path basedir;
	private final Function<Data,String> pathFn;
	
	public FilesystemType(Path basedir, Function<Data,String> pathFn) {
		this.basedir = basedir;
		this.pathFn = pathFn;
	}

	@Override
	public MutableData call(MutableData data, RouteCollector router) {
		
		String path = pathFn.apply(data);
		
		if (path.startsWith("/")) path = path.substring(1);
		
		File entry = resolveAndCheck(basedir, path).toFile();
		
		if (entry.isDirectory()) {
			router.routeTo("dir");
		} else if (entry.isFile()) {
			router.routeTo("file");
		} else {
			router.routeTo("missing");
		}
		
		return data;
	}

}
