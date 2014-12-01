package reka.net.common.sockets;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.net.ChannelAttrs;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketTopicSubscribeOperation implements Operation {

	private final NetServerManager server;
	private final String topic;;
	private final Function<Data,String> idFn;
	private final NetSettings settings;
	
	public SocketTopicSubscribeOperation(NetServerManager server, NetSettings settings, String topic, Function<Data,String> idFn) {
		this.server = server;
		this.topic = topic;
		this.idFn = idFn;
		this.settings = settings;
	}
	
	@Override
	public void call(MutableData data) {
		server.channel(settings, idFn.apply(data), channel -> {
			channel.attr(ChannelAttrs.topics).get().add(topic);
		});
	}
	
}