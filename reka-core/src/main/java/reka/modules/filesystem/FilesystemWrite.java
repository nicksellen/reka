package reka.modules.filesystem;

import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class FilesystemWrite implements Operation {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final java.nio.file.Path basedir;
	private final Function<Data,Path> dataPathFn;
	private final Function<Data,String> filenameFn;
	
	public FilesystemWrite(java.nio.file.Path basedir, Function<Data,Path> dataPathFn, Function<Data,String> filenameFn) {
		this.basedir = basedir;
		this.dataPathFn = dataPathFn;
		this.filenameFn = filenameFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		
		Path dataIn = dataPathFn.apply(data);
		
		String filename = filenameFn.apply(data);
		
		java.nio.file.Path to = resolveAndCheck(basedir, filename);
		
		File toFile = to.toFile();
		
		File parent = toFile.getParentFile();
		
		if (parent.exists() && parent.isFile()) {
			throw runtime("parent of %s is a file", toFile.getAbsolutePath());
		}
		
		try {

			if (!parent.exists()) {
				Files.createDirectories(parent.toPath());
			}
			
			Optional<Content> o = data.at(dataIn).firstContent();
			
			if (o.isPresent()) {

				Content content = o.get();
				
				if (content.hasByteBuffer()) {
					new FileOutputStream(toFile).getChannel().write(content.asByteBuffer());
				} else if (content.hasFile()) {
					File fromFile = content.asFile();
					try (FileInputStream input = new FileInputStream(fromFile);
						 FileOutputStream output = new FileOutputStream(toFile)) {
						input.getChannel().transferTo(
							0, fromFile.length(), 
							output.getChannel());
					}
				} else {
					Files.write(to, content.asUTF8().getBytes(StandardCharsets.UTF_8));
				}
				
			} else {
				logger.error("no content at {} to write!", dataIn.dots());
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}

}
