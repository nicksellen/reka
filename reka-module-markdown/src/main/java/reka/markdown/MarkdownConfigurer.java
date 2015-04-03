package reka.markdown;

import static reka.util.Path.dots;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;
import reka.util.StringWithVars;

public class MarkdownConfigurer implements OperationConfigurer {

	private Function<Data,Path> outFn, inFn;
	
	@Conf.Val
	public void val(String val) {
		in(val);
	}
	
	@Conf.At("in")
	@Conf.At("from")
	public void in(String val) {
		if (StringWithVars.hasVars(val)) {
			inFn = StringWithVars.compile(val).andThen(v -> dots(v));
		} else {
			Path path = dots(val);
			inFn = data -> path;
		}
		if (outFn == null) outFn = inFn;
	}
	
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		if (StringWithVars.hasVars(val)) {
			outFn = StringWithVars.compile(val).andThen(v -> dots(v));
		} else {
			Path path = dots(val);
			outFn = data -> path;
		}
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("convert", () -> new MarkdownOperation(inFn, outFn));
	}

}
