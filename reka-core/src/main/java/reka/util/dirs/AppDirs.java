package reka.util.dirs;

import static java.lang.String.format;
import static reka.api.Path.slashes;
import static reka.util.Util.decode32;
import static reka.util.Util.encode32;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.app.PathAndVersion;

public class AppDirs extends AbstractDirs implements Dirs {
	
	private static final Logger log = LoggerFactory.getLogger(AppDirs.class);
	
	public static String dirnameFor(reka.api.Path appPath, int version) {
		return toDir(PathAndVersion.create(appPath, version));
	}
	
	public static String dirnameFor(reka.api.Path appPath) {
		return format("%s", encode32(appPath.slashes()));
	}
	
	public static String toDir(PathAndVersion pathAndVersion) {
		return format("%s/%d", encode32(pathAndVersion.path().slashes()), pathAndVersion.version());
	}
	
	public static Map<PathAndVersion,Path> listApps(BaseDirs dirs) {
		Map<reka.api.Path,Integer> appVersions = new HashMap<>();
		Map<reka.api.Path,java.nio.file.Path> appPaths = new HashMap<>();
		try {
			Files.list(dirs.app()).forEach(identityPath -> {
				reka.api.Path path;
				try {
					path = slashes(decode32(identityPath.getFileName().toString()));
				} catch (Throwable t) {
					log.info("not base32, ignoring: {}", identityPath.toAbsolutePath().toString());
					return;
				}
				try {
					Files.list(identityPath).forEach(versionPath -> {
						try {
							int v = Integer.valueOf(versionPath.getFileName().toString());
							Integer existingVersion = appVersions.get(path);
							if (existingVersion == null || existingVersion < v) {
								appVersions.put(path, v);
								appPaths.put(path, versionPath);
							}
						} catch (NumberFormatException e) {
							// ignore
						}
					});
				} catch (Exception e1) {
					throw unchecked(e1);
				}
			});
		} catch (IOException e) {
			throw unchecked(e);
		}
		Map<PathAndVersion, Path> result = new HashMap<>();
		appPaths.forEach((path, filepath) -> {
			result.put(PathAndVersion.create(path, appVersions.get(path)), filepath);
		});
		return result;
	}
	 
	protected AppDirs(reka.api.Path appPath, Path app, Path data, Path tmp, BaseDirs basedirs) {
		super(app, data, tmp);
		this.appPath = appPath;
		this.basedirs = basedirs;
	}

	private final reka.api.Path appPath;
	private final BaseDirs basedirs;
		
	public reka.api.Path appPath() {
		return appPath;
	}
	
	public BaseDirs basedirs() {
		return basedirs;
	}
	
}
