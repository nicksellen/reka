package reka.modules.filesystem;

import static reka.util.Path.dots;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class FilesystemWriteConfigurer implements OperationConfigurer {

	private final java.nio.file.Path basedir;
	
	private Function<Data,reka.util.Path> dataPathFn;
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
		ops.add("files/write", () -> new FilesystemWrite(basedir, dataPathFn, filenameFn));
	}
	
}