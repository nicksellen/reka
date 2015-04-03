package reka.net.http.server;

import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.utf8;
import static reka.util.Path.path;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.util.Path;

@Sharable
public class HttpResponseToDatasetDecoder extends MessageToMessageDecoder<FullHttpResponse> {

	private static final Path STATUS = path("status");
	private static final Path CONTENT = path("content");
	private static final Path HEADERS = path("headers");
	
	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpResponse response, List<Object> out) throws Exception {
		
		final MutableData data = MutableMemoryData.create();

		ByteBuf content = response.content();
		byte[] contentBytes = new byte[content.readableBytes()];
		content.readBytes(contentBytes);
		
		data
			.put(STATUS, integer(response.getStatus().code()))
			.put(CONTENT, utf8(new String(contentBytes, StandardCharsets.UTF_8)));
		
		MutableData headers = data.createMapAt(HEADERS);

		// headers

		for (Entry<String, String> header : response.headers()) {
			headers.put(Path.path(header.getKey()), utf8(header.getValue()));
		}
		
		out.add(data);
	}

}
