package reka.modules.builtins;

import static reka.util.Path.path;
import static reka.util.Path.root;
import reka.config.processor.CommentConverter;
import reka.config.processor.DocumentationConverter;
import reka.config.processor.IncludeConverter;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.modules.builtins.adder.AdderConfigurer;
import reka.modules.filesystem.FilesystemConfigurer;
import reka.modules.json.JsonModule.JsonConfigurer;
import reka.util.Path;

public class BuiltinsModule implements Module {

	@Override
	public Path base() {
		return root();
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		
		module.main(() -> new BuiltinsConfigurer());
		
		module.submodule(path("json"), () -> new JsonConfigurer());
		module.submodule(path("fs"), () -> new FilesystemConfigurer());
		
		module.submodule(path("timer"), () -> new TimerConfigurer());
		module.submodule(path("adder"), () -> new AdderConfigurer());
		
		// the ordering of these is very important! be careful :)
		
		module.converter(new CommentConverter());
		module.converter(new EnvConverter());
		module.converter(new EachConverter()); 
		module.converter(new IncludeConverter());
		module.converter(new DocumentationConverter());
		
	}

}
