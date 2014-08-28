package reka.builtins;

import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.MarkdownConverter;
import reka.config.processor.CommentConverter;
import reka.config.processor.DocConverter;
import reka.config.processor.IncludeConverter;
import reka.core.bundle.RekaBundle;

public class BuiltinsBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(root(), () -> new BuiltinsModule());
		bundle.module(path("timer"), () -> new TimerModule());
		bundle.converter(new CommentConverter());
		bundle.converter(new EachConverter()); 
		bundle.converter(new IncludeConverter());
		bundle.converter(new MarkdownConverter());
		bundle.converter(new DocConverter());
		bundle.converter(new EnvConverter());
	}

}
