package reka.net.http.operations;

import java.nio.file.Path;

import reka.api.Path.Response;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.net.http.operations.HttpContentUtils.ContentAndType;

public class HttpContent implements Operation {
	
	private final Content content, contentType;
	
	protected HttpContent(Path tmpdir, Content content, String contentType) {
		ContentAndType vals = HttpContentUtils.convert(tmpdir, content, contentType);
		this.content = vals.content();
		this.contentType = vals.type();
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.put(Response.CONTENT, content)
		    .put(Response.Headers.CONTENT_TYPE, contentType);
	}

}
