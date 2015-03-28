package reka.net.common.sockets;

import static java.lang.String.format;
import reka.Identity;
import reka.api.data.MutableData;
import reka.core.setup.StatusDataProvider;
import reka.net.ChannelAttrs;
import reka.net.NetManager;
import reka.net.NetSettings.Type;

public class SocketStatusProvider implements StatusDataProvider {

	private final NetManager server;
	private final Identity identity;
	private final Type type;
	
	public SocketStatusProvider(NetManager server, Identity identity, Type type) {
		this.server = server;
		this.identity = identity;
		this.type = type;
	}
	
	@Override
	public boolean up() {
		return true;
	}

	@Override
	public void statusData(MutableData data) {
		long conns = server.channels(identity).withAttr(ChannelAttrs.type, type).count();
		data.putLong("connections", conns);
		data.putString("summary", format("conns:%d", conns));
	}
	
}