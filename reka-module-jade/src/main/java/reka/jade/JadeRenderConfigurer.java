package reka.jade;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.api.Path.dots;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.exceptions.JadeException;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;

public class JadeRenderConfigurer implements OperationConfigurer {
	
	private JadeTemplate template;
	
	@SuppressWarnings("unused")
	private Boolean cache = false; // TODO: fix up the caching
	
	private Path in = Path.empty(), out;
	
	@Conf.Config
	@Conf.At("template")
	public void template(Config config) {
	    if (config.hasDocument()) {
	        checkArgument("jade".equals(config.documentType()), "template must be of type [jade]");
            template = compile(new String(config.documentContent(), StandardCharsets.UTF_8));
            if (config.hasValue() && out == null) {
            	out = dots(config.valueAsString());
            }
        } else if (config.hasValue()) {
            template = compile(config.valueAsString());
        }
	}
	
	private JadeTemplate compile(String content) {
		JadeConfiguration jade = new JadeConfiguration();
		jade.setTemplateLoader(new TemplateLoader() {

			public Reader getReader(String name) throws IOException {
				// we're not using their stupid loader
				// we ALWAYS return the same text regardless of the name
				// used
				// as this loader only ever gets used for this template
				return new StringReader(content);
			}

			public long getLastModified(String name) throws IOException {
				return 0;
			}

		});
		
		try {
			return jade.getTemplate(null);
		} catch (JadeException | IOException e) {
			throw unchecked(e);
		}
	}
	
	@Conf.At("out")
	@Conf.At("into")
	public void out(String value) {
		out = dots(value);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		if (out == null) out = Path.Response.CONTENT;
	    ops.add("jade", () -> new JadeRender(template, in, out));
	}
	
}
