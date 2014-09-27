package reka.less;

import static reka.api.Path.path;

import org.lesscss.LessCompiler;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

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
