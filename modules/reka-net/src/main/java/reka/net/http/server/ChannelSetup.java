package reka.net.http.server;

import reka.Identity;

public interface ChannelSetup <T> {
	Runnable add(String host, Identity identity, T flows);
	Runnable pause(String host);
	void resume(String host);
	boolean isEmpty();
}
