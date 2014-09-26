package reka.less;

import static reka.api.Path.path;

import org.lesscss.LessCompiler;

import reka.api.Path;
import reka.core.bundle.BundleConfigurer;

public class LessBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("less");
	}
	
	private static final LessCompiler compiler;
	
	static {
		compiler = new LessCompiler();
		compiler.setCompress(true);
	}
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new LessModule(compiler));
	}

}
