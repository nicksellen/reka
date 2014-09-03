package reka.less;

import static reka.api.Path.path;

import org.lesscss.LessCompiler;

import reka.core.bundle.RekaBundle;

public class LessBundle implements RekaBundle {
	
	private static final LessCompiler compiler;
	
	static {
		compiler = new LessCompiler();
		compiler.setCompress(true);
	}
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("less"), () -> new LessModule(compiler));
	}

}
