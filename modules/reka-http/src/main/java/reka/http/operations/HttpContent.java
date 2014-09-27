package reka.http.operations;

import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class HttpContent extends BaseHttpContent implements Operation {
	
	public HttpContent(Content content, Content contentType) {
		super(content, contentType);
	}

	@Override
	public void call(MutableData data) {
		data.put(Response.CONTENT, content)
		    .put(Response.Headers.CONTENT_TYPE, contentType);
	}

}
