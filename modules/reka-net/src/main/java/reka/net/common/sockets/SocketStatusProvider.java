package reka.net.common.sockets;

import static java.lang.String.format;
import reka.api.data.MutableData;
import reka.core.setup.StatusDataProvider;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class SocketStatusProvider implements StatusDataProvider {

	private final NetServerManager server;
	private final NetSettings settings;
	
	public SocketStatusProvider(NetServerManager server, NetSettings settings) {
		this.server = server;
		this.settings = settings;
	}
	
	@Override
	public boolean up() {
		return true;
	}

	@Override
	public void statusData(MutableData data) {
		server.channels(settings, channels -> {
			int conns = channels.size();
			data.putInt("connections", conns);
			data.putString("summary", format("conns:%d", conns));
		});
	}
	
}