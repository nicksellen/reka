package reka.net.http.operations;

import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.utf8;

import java.util.function.Function;

import reka.api.Path.Response;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class HttpRedirectOperation implements Operation {

	private final Function<Data,String> urlFn;
	private final Content status;
	private final Content content = utf8("");
	
	public HttpRedirectOperation(Function<Data,String> urlFn, boolean temporary) {
		this.urlFn = urlFn;
		status = integer(temporary ? 302 : 301);
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.put(Response.STATUS, status)
			.put(Response.CONTENT, content)
			.putString(Response.Headers.LOCATION, urlFn.apply(data));
	}

}
