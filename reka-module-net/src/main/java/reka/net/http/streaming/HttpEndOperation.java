package reka.net.http.streaming;

import io.netty.handler.codec.http.LastHttpContent;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.net.NetModule;


public class HttpEndOperation implements Operation {

	@Override
	public void call(MutableData data, OperationContext ctx) {
		ctx.lookup(NetModule.Keys.channel).ifPresent(channel -> {
			channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
		});
	}

}
