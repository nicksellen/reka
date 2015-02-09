package reka.builtins;

import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.api.Path;
import reka.config.processor.CommentConverter;
import reka.config.processor.DocumentationConverter;
import reka.config.processor.IncludeConverter;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class BuiltinsModule implements Module {

	@Override
	public Path base() {
		return root();
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		
		module.main(() -> new BuiltinsConfigurer());
		
		module.submodule(path("timer"), () -> new TimerConfigurer());
		
		// the ordering of these is very important! be careful :)
		
		module.converter(new CommentConverter());
		module.converter(new EnvConverter());
		module.converter(new EachConverter()); 
		module.converter(new IncludeConverter());
		module.converter(new DocumentationConverter());
		
	}

}
