package reka.http.server;

import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.Response;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

@Sharable
public class HttpResponseToDatasetDecoder extends MessageToMessageDecoder<FullHttpResponse> {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

	@Override
	protected void decode(ChannelHandlerContext ctx, FullHttpResponse response, List<Object> out) throws Exception {
		
		final MutableData data = MutableMemoryData.create();

		ByteBuf content = response.content();
		byte[] contentBytes = new byte[content.readableBytes()];
		content.readBytes(contentBytes);
		//ByteBuffer bb = ByteBuffer.allocateDirect(content.readableBytes());
		//content.readBytes(bb);
		
		data
			.put(Response.STATUS, integer(response.getStatus().code()))
			.put(Response.CONTENT, utf8(new String(contentBytes, StandardCharsets.UTF_8)));
		
		MutableData headers = data.createMapAt(Response.HEADERS);

		// headers

		for (Entry<String, String> header : response.headers()) {
			headers.put(Path.path(header.getKey()), utf8(header.getValue()));
		}
		
		//logger.info("  decoded response as {}", ContentStores.writeToStringAsPrettyJson(data));
		
		out.add(data);
	}



}
