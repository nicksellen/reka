package reka.less;

import static reka.util.Path.path;

import org.lesscss.LessCompiler;

import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

public class LessModule implements Module {

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
	public void setup(ModuleDefinition module) {
		module.main(() -> new LessConfigurer(compiler));
	}

}
