package reka.http.configurers;

import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.api.content.Content;
import reka.api.content.Content.BinaryContent;
import reka.api.content.Contents;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.http.operations.HttpContentWithETag;

public class HttpContentConfigurer implements Supplier<FlowSegment> {
	
	private Content content;
	private Content contentType;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasData()) {
			checkConfig(config.dataType() != null, "no data type!");
			content = Contents.binary(config.dataType(), config.data());
			if (config.dataType() != null) {
				contentType = Contents.utf8(config.dataType());
			}
		} else if (config.hasDocument()) {
			checkConfig(config.documentType() != null, "no document type!");
			content = binary(config.documentType(), config.documentContent());
			if (config.documentType() != null) {
				contentType = Contents.utf8(config.documentType());
			}
		} else if (config.hasValue()) {
			content = utf8(config.valueAsString());
			contentType = utf8("text/plain");
		}
	}
	
	@Conf.At("content")
	public HttpContentConfigurer content(String value) {
		content = utf8(value);
		return this;
	}

	public HttpContentConfigurer content(Data value) {
		content = utf8(value.toJson());
		contentType = utf8("application/json");
		return this;
	}

	@Conf.At("content-type")
	public HttpContentConfigurer contentType(String value) {
		contentType = utf8(value);
		return this;
	}
	
	@Override
	public FlowSegment get() {
		if (content instanceof BinaryContent && contentType == null) {
			String ct = ((BinaryContent) content).contentType();
			if (ct != null) {
				contentType = utf8(ct);
			}
		}
		return sync("http-content", () -> new HttpContentWithETag(content, contentType));
	}

}
