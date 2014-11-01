package reka.http.operations;

import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.http.operations.HttpContentUtils.ContentAndType;

public class HttpContent implements Operation {
	
	private final Content content, contentType;
	
	protected HttpContent(Content content, String contentType) {
		ContentAndType vals = HttpContentUtils.convert(content, contentType);
		this.content = vals.content();
		this.contentType = vals.type();
	}

	@Override
	public void call(MutableData data) {
		data.put(Response.CONTENT, content)
		    .put(Response.Headers.CONTENT_TYPE, contentType);
	}

}
