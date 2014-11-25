package reka.admin;

import static reka.util.Util.deleteRecursively;
import static reka.util.Util.ignoreExceptions;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.AppDirs;
import reka.BaseDirs;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class RekaUnpackOperation implements Operation {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final BaseDirs basedirs;
	private final Function<Data, Path> dataPathFn;
	private final Function<Data, String> identityFn;

	public RekaUnpackOperation(AppDirs dirs, Function<Data, Path> dataPathFn, Function<Data, String> identityFn) {
		this.basedirs = dirs.basedirs();
		this.dataPathFn = dataPathFn;
		this.identityFn = identityFn;
	}

	@Override
	public void call(MutableData data) {

		Path dataPath = dataPathFn.apply(data);
		String identity = identityFn.apply(data);

		data.putString("identity", identity);

		AppDirs dirs = basedirs.resolve(identity);

		Data val = data.at(dataPath);

		if (!val.isPresent()) throw runtime("no data at %s", dataPath.dots());
		if (!val.isContent()) throw runtime("not content at %s", dataPath.dots());

		log.info("unpacking {} to {}", identity, dirs.app());

		deleteRecursively(dirs.app());
		dirs.mkdirs();

		unpack(val.content().asBytes(), dirs.app());

	}

	private static void unpack(byte[] bytes, java.nio.file.Path dest) {
		try {

			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				java.nio.file.Path filepath = dest.resolve(e.getName());
				Files.createDirectories(filepath.getParent());
				FileOutputStream out = new FileOutputStream(filepath.toFile());
				try {
					byte[] buf = new byte[8192];
					int len;
					while ((len = zip.read(buf, 0, buf.length)) > 0) {
						out.write(buf, 0, len);
					}
				} finally {
					ignoreExceptions(() -> out.close());
					ignoreExceptions(() -> zip.closeEntry());
				}
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}

}