package reka.http.configurers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import reka.api.data.Data;
import reka.api.flow.FlowOperationConfigurer;
import reka.config.configurer.annotations.Conf;
import reka.http.operations.HttpContents;

public class HttpContentsBuilder implements FlowOperationConfigurer<HttpContents> {

	private Map<String, HttpContents.ContentItem> contents = new HashMap<>();
	
	private boolean useRouting = false;

	@Conf.At("routing")
	public HttpContentsBuilder routing(boolean value) {
		useRouting = value;
		return this;
	}
	
	@Conf.At("content")
	public HttpContentsBuilder content(String path, ContentItemBuilder info) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		contents.put(path, info.build());
		return this;
	}

	@Override
	public HttpContents build() {
		return useRouting ? new HttpContents.Routed(contents) : new HttpContents.Basic(contents);
	}

	public class ContentItemBuilder {
		
		private String content;
		private String contentType;
		
		@Conf.At("content")
		public ContentItemBuilder content(String value) {
			content = value;
			return this;
		}

		@Conf.At("content")
		public ContentItemBuilder content(Data value) {
			content = value.toJson();
			return this;
		}
		
		@Conf.At("content-type")
		public ContentItemBuilder contentType(String value) {
			contentType = value;
			return this;
		}
		
		public HttpContents.ContentItem build() {
			return new HttpContents.ContentItem(content.getBytes(StandardCharsets.UTF_8), contentType);
		}
		
	}
	

}
