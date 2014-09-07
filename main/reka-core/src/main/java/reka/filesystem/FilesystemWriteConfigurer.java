package reka.filesystem;

import static reka.api.Path.dots;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class FilesystemWriteConfigurer implements OperationsConfigurer {

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
		ops.add("files/write", store -> new FilesystemWrite(basedir, dataPathFn, filenameFn));
	}
	
}