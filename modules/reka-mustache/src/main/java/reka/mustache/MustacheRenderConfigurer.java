package reka.mustache;

import static reka.api.Path.dots;
import reka.api.Path;
import reka.api.Path.Response;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class MustacheRenderConfigurer implements OperationConfigurer {

	private String template;
	private Path in, out;
	
	@Conf.Config
	@Conf.At("template")
	public void template(Config config) {
	    if (config.hasDocument()) {
            template = config.documentContentAsString();
            if (config.hasValue() && out == null) {
            	out = dots(config.valueAsString());
            }
        } else if (config.hasValue()) {
            template = config.valueAsString();
        }
	}
	
	@Conf.At("out")
	@Conf.At("into")
	public void out(String value) {
		out = dots(value);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		if (in == null) in = Path.empty();
		if (out == null) out = Response.CONTENT;
		ops.add("render", ctx -> new MustacheRenderOperation(template, in, out));
	}

}
