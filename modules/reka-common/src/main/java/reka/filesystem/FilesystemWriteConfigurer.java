package reka.filesystem;

import static reka.api.Path.dots;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class FilesystemWriteConfigurer implements OperationConfigurer {

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
	public void setup(OperationSetup ops) {
		ops.add("files/write", ctx -> new FilesystemWrite(basedir, dataPathFn, filenameFn));
	}
	
}