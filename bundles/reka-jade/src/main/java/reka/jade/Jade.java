package reka.jade;

import java.io.StringWriter;

import reka.api.Path;
import reka.api.Path.Response;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import de.neuland.jade4j.model.JadeModel;
import de.neuland.jade4j.template.JadeTemplate;

public class Jade implements SyncOperation {
	
	private final JadeTemplate template;
	private final Path inputPath;
	private final Path outputPath;
	private final boolean mainResponse;

	public Jade(JadeTemplate template, Path inputPath, Path outputPath) {
		this.template = template;
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.mainResponse = outputPath.equals(Response.CONTENT);
	}

	@Override
	public MutableData call(MutableData data) {
		if (mainResponse) data.putString(Response.Headers.CONTENT_TYPE, "text/html");
		StringWriter writer = new StringWriter();
        template.process(new JadeModel(data.at(inputPath).viewAsMap()), writer);
	    return data.putString(outputPath, writer.toString());
	}
	
}