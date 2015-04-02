package reka.markdown;

import static java.util.Arrays.asList;

import java.nio.charset.StandardCharsets;

import reka.config.Config;
import reka.config.processor.ConfigConverter;

public class MarkdownConverter implements ConfigConverter {

	@Override
	public void convert(Config config, Output out) {
		if (config.hasDocument() && asList("markdown", "md").contains(config.documentType())) {
			
			out.add(config.toBuilder()
				.document("text/html", 
						MarkdownModuleConfigurer.md.get().markdownToHtml(config.documentContentAsString()).getBytes(StandardCharsets.UTF_8))
				.build());
			
		} else {
			out.add(config);
		}
	}

}
