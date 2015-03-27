package reka.net.common.sockets;

import static java.lang.String.format;
import reka.Identity;
import reka.api.data.MutableData;
import reka.core.setup.StatusDataProvider;
import reka.net.ChannelAttrs;
import reka.net.ChannelAttrs.AttributeMatcher;
import reka.net.NetServerManager;

public class SocketStatusProvider implements StatusDataProvider {

	private final NetServerManager server;
	private final Identity identity;
	
	public SocketStatusProvider(NetServerManager server, Identity identity) {
		this.server = server;
		this.identity = identity;
	}
	
	@Override
	public boolean up() {
		return true;
	}

	@Override
	public void statusData(MutableData data) {
		long conns = server.channels(identity).count();
		data.putLong("connections", conns);
		data.putString("summary", format("conns:%d", conns));
	}
	
}