package reka.net.common.sockets;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketTopicSendOperation implements Operation {

	private final NetServerManager server;
	private final String topic;
	private final Function<Data,String> messageFn;
	private final NetSettings settings;
	
	public SocketTopicSendOperation(NetServerManager server, NetSettings settings, String topic, Function<Data,String> messageFn) {
		this.server = server;
		this.topic = topic;
		this.messageFn = messageFn;
		this.settings = settings;
	}
	
	@Override
	public void call(MutableData data) {
		server.channels(settings, channels -> {
			channels.writeAndFlush(new TextWebSocketFrame(messageFn.apply(data)));
		});
	}
	
}