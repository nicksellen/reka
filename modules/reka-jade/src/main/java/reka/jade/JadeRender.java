package reka.jade;

import java.io.StringWriter;

import reka.api.Path;
import reka.api.Path.Response;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import de.neuland.jade4j.model.JadeModel;
import de.neuland.jade4j.template.JadeTemplate;

public class JadeRender implements Operation {
	
	private final JadeTemplate template;
	private final Path in;
	private final Path out;
	private final boolean mainResponse;

	public JadeRender(JadeTemplate template, Path in, Path out) {
		this.template = template;
		this.in = in;
		this.out = out;
		this.mainResponse = out.equals(Response.CONTENT);
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		if (mainResponse) data.putString(Response.Headers.CONTENT_TYPE, "text/html");
		StringWriter writer = new StringWriter();
        template.process(new JadeModel(data.at(in).viewAsMap()), writer);
	    data.putString(out, writer.toString());
	}
	
}