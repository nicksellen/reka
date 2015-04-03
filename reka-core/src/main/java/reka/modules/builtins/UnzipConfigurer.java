package reka.modules.builtins;

import static reka.api.Path.dots;

import java.io.File;
import java.util.function.Function;

import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class UnzipConfigurer implements OperationConfigurer {

	private Function<Data,Path> dataPathFn;
	private Function<Data,java.nio.file.Path> outputDirFn;
	
	@Conf.At("data")
	@Conf.At("from")
	public void in(String val) {
		dataPathFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}
	
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		outputDirFn = StringWithVars.compile(val).andThen(s -> new File(s).toPath());
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("unzip", () -> new UnzipOperation(dataPathFn, outputDirFn));
	}

}
