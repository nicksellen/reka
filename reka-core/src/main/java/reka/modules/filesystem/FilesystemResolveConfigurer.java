package reka.modules.filesystem;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class FilesystemResolveConfigurer implements OperationConfigurer {
	
	private final java.nio.file.Path basedir;
	
	public FilesystemResolveConfigurer(java.nio.file.Path basedir) {
		this.basedir = basedir;
	}
	
	private Function<Data,String> inFn;
	private Function<Data,String> outFn;
	
	@Conf.At("in")
	@Conf.At("from")
	public void in(String val) {
		inFn = StringWithVars.compile(val);
	}
	
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		outFn = StringWithVars.compile(val);
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("resolve", () -> new FilesystemResolveOperation(basedir, inFn, outFn));
	}

}
