package reka.dirs;

import static java.lang.String.format;
import static reka.util.Util.decode32;
import static reka.util.Util.encode32;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import reka.core.app.IdentityAndVersion;

public class AppDirs extends AbstractDirs implements Dirs {
	
	public static String dirnameFor(String identity, int version) {
		return toDir(IdentityAndVersion.create(identity, version));
	}
	
	public static String dirnameFor(String identity) {
		return format("%s", encode32(identity));
	}
	
	public static String toDir(IdentityAndVersion identityAndVersion) {
		return format("%s/%d", encode32(identityAndVersion.identity()), identityAndVersion.version());
	}
	
	public static Map<IdentityAndVersion,Path> listApps(BaseDirs dirs) {
		Map<String,Integer> appVersions = new HashMap<>();
		Map<String,java.nio.file.Path> appPaths = new HashMap<>();
		try {
			Files.list(dirs.app()).forEach(identityPath -> {
				String identity = decode32(identityPath.getFileName().toString());
				try {
					Files.list(identityPath).forEach(versionPath -> {
						try {
							int v = Integer.valueOf(versionPath.getFileName().toString());
							Integer existingVersion = appVersions.get(identity);
							if (existingVersion == null || existingVersion < v) {
								appVersions.put(identity, v);
								appPaths.put(identity, versionPath);
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
		Map<IdentityAndVersion, Path> result = new HashMap<>();
		appPaths.forEach((identity, path) -> {
			result.put(IdentityAndVersion.create(identity, appVersions.get(identity)), path);
		});
		return result;
	}
	 
	protected AppDirs(Path app, Path data, Path tmp, BaseDirs basedirs) {
		super(app, data, tmp);
		this.basedirs = basedirs;
	}

	private final BaseDirs basedirs;
		
	public BaseDirs basedirs() {
		return basedirs;
	}
	
}
