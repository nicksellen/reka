package reka.builtins;

import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.config.processor.CommentConverter;
import reka.config.processor.DocumentationConverter;
import reka.config.processor.IncludeConverter;
import reka.core.bundle.BundleConfigurer;

public class BuiltinsBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(root(), () -> new BuiltinsModule());
		bundle.module(path("timer"), () -> new TimerModule());
		
		// the ordering of these is very important! be careful :)
		
		bundle.converter(new CommentConverter());
		bundle.converter(new EnvConverter());
		bundle.converter(new EachConverter()); 
		bundle.converter(new IncludeConverter());
		bundle.converter(new DocumentationConverter());
		
	}

}
