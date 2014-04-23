package reka.filesystem;

import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class FilesystemResolveConfigurer implements Supplier<FlowSegment> {
	
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
	public FlowSegment get() {
		return sync("resolve", () -> new FilesystemResolveOperation(basedir, inFn, outFn));
	}

}
