package reka.mustache;

import java.io.StringReader;
import java.io.StringWriter;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class MustacheOp implements Operation {
	
	private final static MustacheFactory mf = new DefaultMustacheFactory();
	
	private final Mustache mustache;
	private final Path in, out;
	
	public MustacheOp(String template, Path inputPath, Path outputPath) {
		mustache = mf.compile(new StringReader(template), "template");
		this.in = inputPath;
		this.out = outputPath;
	}

	@Override
	public MutableData call(MutableData data) {
		StringWriter writer = new StringWriter();
		mustache.execute(writer, data.at(in).viewAsMap());
		data.putString(out, writer.toString());
		return data;
	}

}
