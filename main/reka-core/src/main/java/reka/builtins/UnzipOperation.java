package reka.builtins;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

import com.google.common.base.Charsets;

public class UnzipOperation implements SyncOperation {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Function<Data,Path> dataPathFn;
	private final Function<Data,java.nio.file.Path> outputDirFn;
	
	public UnzipOperation(Function<Data,Path> dataPathFn, Function<Data,java.nio.file.Path> outputDirFn) {
		this.dataPathFn = dataPathFn;
		this.outputDirFn = outputDirFn;
	}
	
	@Override
	public MutableData call(MutableData data) {
		Path dataPath = dataPathFn.apply(data);
		java.nio.file.Path outputDir = outputDirFn.apply(data);
		
		Data val = data.at(dataPath);
		
		if (!val.isPresent()) {
			throw runtime("no data at %s", dataPath.dots());
		}
		
		if (!val.isContent()) {
			throw runtime("not content at %s", dataPath.dots());
		}
		
		try {
			
			byte[] bytes = val.content().asBytes();
			log.info("unzip {} bytes", bytes.length);
			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), Charsets.UTF_8);
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				String name = e.getName();
				java.nio.file.Path filepath = outputDir.resolve(e.getName());
				log.info(" - {} -> {}", name, filepath);
				Files.createDirectories(filepath.getParent());
				FileOutputStream fout = new FileOutputStream(filepath.toFile());
		        for (int c = zip.read(); c != -1; c = zip.read()) {
		          fout.write(c);
		        }
		        zip.closeEntry();
		        fout.close();
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
		
		return data;
	}

}
