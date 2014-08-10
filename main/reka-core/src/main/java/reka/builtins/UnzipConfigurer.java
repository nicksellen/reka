package reka.builtins;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class UnzipConfigurer implements Supplier<FlowSegment> {

	private Function<Data,Path> dataPathFn;
	private Function<Data,java.nio.file.Path> outputDirFn;
	
	@Conf.At("data")
	public void in(String val) {
		dataPathFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}
	
	@Conf.At("out")
	public void out(String val) {
		outputDirFn = StringWithVars.compile(val).andThen(s -> new File(s).toPath());
	}
	
	@Override
	public FlowSegment get() {
		return sync("unzip", () -> new UnzipOperation(dataPathFn, outputDirFn));
	}

}
