package reka.irc;

import static java.util.Collections.synchronizedList;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

public class RekaBot {
	
	public static interface IrcListener {
		void onMessage(String channel, String sender, String login, String hostname, String message);
		void onConnect();
	}
	
	private class Bot extends PircBot {
		Bot(String name) {
			setName(name);
		}

		@Override
		protected void onConnect() {
			super.onConnect();
			listeners.forEach(IrcListener::onConnect);
		}

		@Override
		protected void onMessage(String channel, String sender, String login, String hostname, String message) {
			super.onMessage(channel, sender, login, hostname, message);
			listeners.forEach(listener -> {
				listener.onMessage(channel, sender, login, hostname, message);
			});
		}
	}
	
	private final String hostname;
	private final String channel;
	private final String key;
	
	private final List<IrcListener> listeners = synchronizedList(new ArrayList<>());

	private final Bot bot;
	
	public RekaBot(String name, String hostname, String channel, String key) {
		this.hostname = hostname;
		this.channel = channel;
		this.key = key;
		this.bot = new Bot(name);
		bot.setVerbose(true);
	}
	
	public RekaBot addListener(IrcListener listener) {
		this.listeners.add(listener);
		return this;
	}
	
	public void connect() {
		try {
			bot.connect(hostname);
			if (key != null) {
				bot.joinChannel(channel, key);
			} else {
				bot.joinChannel(channel);
			}
		} catch (IOException | IrcException e) {
			throw unchecked(e);
		}
	}
	
	public void send(String message) {
		bot.sendMessage(channel, message);
	}
	
	public void shutdown() {
		bot.disconnect();
		bot.dispose();
	}
	
	
	
}
