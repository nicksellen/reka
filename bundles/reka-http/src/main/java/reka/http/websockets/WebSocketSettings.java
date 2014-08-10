package reka.http.websockets;

import java.util.Arrays;

public class WebSocketSettings {
	
	private final int[] ports;
	
	public WebSocketSettings(int[] ports) {
		this.ports = ports;
	}
	
	public int[] ports() {
		return ports;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(ports);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WebSocketSettings)) {
			return false;
		}
		WebSocketSettings other = (WebSocketSettings) obj;
		return Arrays.equals(ports, other.ports);
	}

}
