package reka.net.http.streaming;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.NetModule;
import reka.net.http.server.DataToHttpEncoder;

public class HttpHeadOperation implements Operation {
	
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void call(MutableData data, OperationContext ctx) {
		ctx.lookup(NetModule.Keys.channel).ifPresent(channel -> {
			
			// we're going off the rails now, so we don't want to decode the result data
			// TODO: this should be contained in a streaming block config construct
			channel.pipeline().remove(DataToHttpEncoder.class);
			DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
			HttpHeaders.setTransferEncodingChunked(response);
			HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "text/plain");
			channel.writeAndFlush(response);
		});
	}

}
