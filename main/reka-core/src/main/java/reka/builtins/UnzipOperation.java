package reka.builtins;

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

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;

public class UnzipOperation implements Operation {
	
	private final Function<Data,Path> dataPathFn;
	private final Function<Data,java.nio.file.Path> outputDirFn;
	
	public UnzipOperation(Function<Data,Path> dataPathFn, Function<Data,java.nio.file.Path> outputDirFn) {
		this.dataPathFn = dataPathFn;
		this.outputDirFn = outputDirFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		Path dataPath = dataPathFn.apply(data);
		java.nio.file.Path outputDir = outputDirFn.apply(data);
		
		Data val = data.at(dataPath);
		
		if (!val.isPresent()) throw runtime("no data at %s", dataPath.dots());
		if (!val.isContent()) throw runtime("not content at %s", dataPath.dots());
		
		try {
			
			byte[] bytes = val.content().asBytes();
			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				java.nio.file.Path filepath = outputDir.resolve(e.getName());
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
