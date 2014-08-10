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
	public void setup(BundleSetup setup) {
		setup.use(root(), () -> new UseBuiltins());
		setup.use(path("timer"), () -> new UseTimer());
		setup.converter(new CommentConverter()); 
		setup.converter(new IncludeConverter());
		setup.converter(new MarkdownConverter());
		setup.converter(new DocConverter());
		setup.converter(new EnvConverter());
	}

}
