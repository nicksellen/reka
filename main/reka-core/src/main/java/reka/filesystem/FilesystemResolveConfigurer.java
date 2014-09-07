package reka.filesystem;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

public class FilesystemResolveConfigurer implements OperationConfigurer {
	
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
