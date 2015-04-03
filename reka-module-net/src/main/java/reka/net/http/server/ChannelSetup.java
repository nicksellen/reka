package reka.net.http.server;

import reka.util.Identity;

public interface ChannelSetup <T> {
	Runnable add(String host, Identity identity, T flows);
	Runnable pause(String host);
	boolean isEmpty();
}
