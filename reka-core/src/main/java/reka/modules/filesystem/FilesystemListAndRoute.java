package reka.modules.filesystem;

import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Path.path;
import static reka.util.Path.PathElements.index;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.MutableData;
import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;
import reka.util.Path;

public class FilesystemListAndRoute implements RouterOperation {
	
	public static final RouteKey DIRECTORY = RouteKey.named("directory");
	public static final RouteKey FILE = RouteKey.named("file");
	public static final RouteKey NOT_FOUND = RouteKey.named("not found");
	
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger("filesystem/list");

	private final java.nio.file.Path basedir;
	private final Path dirField;
	private final Path field;
	
	public FilesystemListAndRoute(java.nio.file.Path basedir, Path dirField, Path field) {
		this.basedir = basedir;
		this.dirField = dirField;
		this.field = field;
	}

	@Override
	public void call(MutableData data, RouteCollector router) {
		String relative = data.getString(dirField).orElse("/");
		if (relative.startsWith("/")) relative = relative.substring(1);
		File entry = resolveAndCheck(basedir, relative).toFile();
		if (!entry.exists()) {
			router.routeTo(NOT_FOUND);
		} else if (entry.isDirectory()) {
			File[] files = entry.listFiles();
			MutableData list = data.createMapAt(field);
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String path = relative.isEmpty() ? file.getName() : relative + "/" + file.getName();
				populate(list.createMapAt(path(index(i))), file, path);
			}
			router.routeTo(DIRECTORY);
		} else {
			populate(data.createMapAt(field), entry, relative);
			router.routeTo(FILE);
		}
	}
	
	private void populate(MutableData item, File file, String path) {
		item.putString("name", file.getName());
		item.putString("path", path);
		item.putLong("size", file.length());
		item.putBool("dir", file.isDirectory());
	}

}
