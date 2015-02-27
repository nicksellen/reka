package reka.net.http;

import io.netty.handler.codec.http.LastHttpContent;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.NetModule;


public class HttpEndOperation implements Operation {

	@Override
	public void call(MutableData data, OperationContext ctx) {
		ctx.lookup(NetModule.Keys.channel).ifPresent(channel -> {
			channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
		});
	}

}
