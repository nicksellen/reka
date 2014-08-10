package reka.filesystem;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class FilesystemWriteConfigurer implements Supplier<FlowSegment> {

	private final java.nio.file.Path basedir;
	
	private Function<Data,reka.api.Path> dataPathFn;
	private Function<Data,String> filenameFn;
	
	public FilesystemWriteConfigurer(java.nio.file.Path basedir) {
		this.basedir = basedir;
	}
	
	@Conf.At("data")
	public void data(String val) {
		dataPathFn = StringWithVars.compile(val).andThen(path -> dots(path));;
	}
	
	@Conf.At("filename")
	public void filename(String val) {
		filenameFn = StringWithVars.compile(val);
	}
	
	@Override
	public FlowSegment get() {
		return sync("files/write", () -> new FilesystemWrite(basedir, dataPathFn, filenameFn));
	}
	
}