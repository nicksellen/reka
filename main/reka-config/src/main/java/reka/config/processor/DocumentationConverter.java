package reka.config.processor;

import static java.util.Arrays.asList;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;

public class DocumentationConverter implements ConfigConverter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static class Doc {
		
		private final String contentType;
		private final byte[] content;
		
		Doc(String contentType, byte[]content) {
			this.contentType = contentType;
			this.content = content;
		}
		
		public String contentType() {
			return contentType;
		}
		
		public byte[] content() {
			return content;
		}
		
	}
	
	private final List<Doc> docs = new ArrayList<>();
	
	private void add(String type, byte[] content) {
		Doc doc;
		if (type == null || "".equals(type)) {
			doc = new Doc("text/plain", content);
		} else {
			doc = new Doc(type, content);
		}
		log.debug("adding @doc -> [{}]\n", doc.content);
		docs.add(doc);
	}
	
	@Override
	public void convert(Config config, Output out) {
		if (config.hasKey() && asList("@doc").contains(config.key())) {
			if (config.hasValue()) {
				add("text/plain", config.valueAsString().getBytes(StandardCharsets.UTF_8));
			} else if (config.hasDocument()) {
				add(config.documentType(), config.documentContent());
			} else {
				throw new RuntimeException("@doc must have value or document");
			}
		} else {
			out.add(config); // passthrough
		}
	}

}
