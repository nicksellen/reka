package reka.http.configurers;

import java.util.HashMap;
import java.util.Map;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.http.operations.HttpResponseOperation;
import reka.nashorn.OperationsConfigurer;

public class HttpResponseConfigurer implements OperationsConfigurer {
	
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
	public void setup(OperationSetup ops) {
		ops.add("http response", store -> new HttpResponseOperation(content, contentType, status, headers));
	}

}
