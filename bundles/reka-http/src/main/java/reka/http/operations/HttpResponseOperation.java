package reka.http.operations;

import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.Operation;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

public class HttpResponseOperation implements Operation {
	
	private final Content content;
	private final Content contentType;
	private final Content status;
	private final Map<String,Content> headers;
	
	public HttpResponseOperation(String content, String contentType, int status, Map<String,String> headers) {
		byte[] contentBytes = content.getBytes(Charsets.UTF_8);
		ByteBuffer bb = ByteBuffer.allocateDirect(contentBytes.length).put(contentBytes);
		bb.flip();
		this.content = binary(contentType, bb);
		this.contentType = utf8(contentType);
		this.status = integer(status);
		ImmutableMap.Builder<String,Content> mapBuilder = ImmutableMap.builder();
		for (Entry<String,String> header : headers.entrySet()) {
			mapBuilder.put(header.getKey(), utf8(header.getValue()));
		}
		this.headers = mapBuilder.build();
	}


	@Override
	public MutableData call(MutableData data) {
		
		for (Entry<String, Content> header : headers.entrySet()) {
			data.put(Response.HEADERS.add(header.getKey()), header.getValue());
		}
		
		return data
			.put(Response.CONTENT, content)
			.put(Response.Headers.CONTENT_TYPE, contentType)
			.put(Response.STATUS, status)
		;
	}
	
}
