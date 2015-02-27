package reka.net.http;

import java.nio.charset.StandardCharsets;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.net.NetModule;

public class HttpWriteOperation implements Operation {

	@Override
	public void call(MutableData data, OperationContext ctx) {
		ctx.lookup(NetModule.Keys.channel).ifPresent(channel -> {
			byte[] bytes = "yay\n".getBytes(StandardCharsets.UTF_8);
			channel.writeAndFlush(channel.alloc().buffer().writeBytes(bytes));
		});
	}

}
