package reka.modules.filesystem;

import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;

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
