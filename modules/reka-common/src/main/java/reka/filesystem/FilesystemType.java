package reka.filesystem;

import static reka.filesystem.FilesystemUtils.resolveAndCheck;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;

public class FilesystemType implements RouterOperation {

	public static RouteKey DIR = RouteKey.named("dir");
	public static RouteKey FILE = RouteKey.named("file");
	public static RouteKey MISSING = RouteKey.named("missing");
	
	private final Path basedir;
	private final Function<Data,String> pathFn;
	
	public FilesystemType(Path basedir, Function<Data,String> pathFn) {
		this.basedir = basedir;
		this.pathFn = pathFn;
	}

	@Override
	public void call(MutableData data, RouteCollector router) {
		
		String path = pathFn.apply(data);
		
		if (path.startsWith("/")) path = path.substring(1);
		
		File entry = resolveAndCheck(basedir, path).toFile();
		
		if (entry.isDirectory()) {
			router.routeTo(DIR);
		} else if (entry.isFile()) {
			router.routeTo(FILE);
		} else {
			router.routeTo(MISSING);
		}
	}

}
