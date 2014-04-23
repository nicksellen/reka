package reka.http.configurers;

import static reka.core.builder.FlowSegments.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.http.operations.HttpResponseOperation;

public class HttpResponseConfigurer implements Supplier<FlowSegment> {
	
	private String content;
	private String contentType;
	private int status = 200;
	private final Map<String,String> headers = new HashMap<>();
	
	@Conf.At("content-type")
	public HttpResponseConfigurer contentType(String value) {
		contentType = value;
		return this;
	}
	
	@Conf.At("content")
	public HttpResponseConfigurer content(String value) {
		content = value;
		return this;
	}
	
	@Conf.At("status")
	public HttpResponseConfigurer status(Integer value) {
		status = value;
		return this;
	}
	
	@Conf.At("headers")
	public HttpResponseConfigurer headers(Data value) {
		value.forEachContent((path, content) -> {
			headers.put(path.last().toString(), content.toString());
		});
		return this;
	}

	@Override
	public FlowSegment get() {
		return sync("http response", () -> new HttpResponseOperation(content, contentType, status, headers));
	}

}
