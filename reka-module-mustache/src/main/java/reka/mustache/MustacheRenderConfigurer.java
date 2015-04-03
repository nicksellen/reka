package reka.mustache;

import static reka.util.Path.dots;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;
import reka.util.Path.Response;

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
		ops.add("render", () -> new MustacheRenderOperation(template, in, out));
	}

}
