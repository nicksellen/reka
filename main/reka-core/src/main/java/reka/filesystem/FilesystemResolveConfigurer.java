package reka.filesystem;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class FilesystemResolveConfigurer implements OperationsConfigurer {
	
	private final java.nio.file.Path basedir;
	
	public FilesystemResolveConfigurer(java.nio.file.Path basedir) {
		this.basedir = basedir;
	}
	
	private Function<Data,String> inFn;
	private Function<Data,String> outFn;
	
	@Conf.At("in")
	public void in(String val) {
		inFn = StringWithVars.compile(val);
	}
	
	@Conf.At("out")
	public void out(String val) {
		outFn = StringWithVars.compile(val);
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("resolve", store -> new FilesystemResolveOperation(basedir, inFn, outFn));
	}

}
