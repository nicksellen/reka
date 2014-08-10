package reka.mustache;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.api.Path;
import reka.api.Path.Response;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;

public class MustacheConfigurer implements Supplier<FlowSegment> {

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
	public void out(String value) {
		out = dots(value);
	}
	
	@Override
	public FlowSegment get() {
		if (in == null) in = Path.empty();
		if (out == null) out = Response.CONTENT;
	    return sync("mustache", () -> new MustacheOp(template, in, out));
	}

}
