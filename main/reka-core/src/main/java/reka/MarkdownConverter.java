package reka;

import static java.util.Arrays.asList;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import reka.config.Config;
import reka.config.processor.ConfigConverter;

import com.google.common.base.Charsets;

public class MarkdownConverter implements ConfigConverter {
	
	private final PegDownProcessor md = new PegDownProcessor(Extensions.ALL);

	@Override
	public void convert(Config config, Output out) {
		if (config.hasDocument() && asList("markdown", "md").contains(config.documentType())) {
			
			out.add(config.toBuilder()
				.document("text/html", 
						  md.markdownToHtml(config.documentContentAsString()).getBytes(Charsets.UTF_8))
				.build());
			
			/*
			out.doc(new KeyAndSubkey(config), 
					config.value(), 
					"text/html", 
					md.markdownToHtml(config.documentContentAsString()).getBytes(Charsets.UTF_8));
					*/
		} else {
			out.add(config);
		}
	}

}
