package reka.common.markdown;

import static reka.api.Path.dots;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class MarkdownConfigurer implements OperationConfigurer {

	private Function<Data,Path> outFn, inFn;
	
	@Conf.Val
	public void val(String val) {
		in(val);
	}
	
	@Conf.At("in")
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
		ops.add("convert", store -> new MarkdownOperation(inFn, outFn));
	}

}
