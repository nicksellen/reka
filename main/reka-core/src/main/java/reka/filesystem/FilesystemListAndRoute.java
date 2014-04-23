package reka.filesystem;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.index;
import static reka.filesystem.FilesystemUtils.resolveAndCheck;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;

public class FilesystemListAndRoute implements RoutingOperation {
	
	public static final String DIRECTORY = "directory";
	public static final String FILE = "file";
	public static final String NOT_FOUND = "not found";
	
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
	public MutableData call(MutableData data, RouteCollector router) {
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
		return data;
	}
	
	private void populate(MutableData item, File file, String path) {
		item.putString("name", file.getName());
		item.putString("path", path);
		item.putLong("size", file.length());
		item.putBool("dir", file.isDirectory());
	}

}
