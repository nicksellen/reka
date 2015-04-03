package reka.irc;

import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.irc.RekaBot.IrcListener;

public class IrcMessageFlowListener implements IrcListener {

	private final Flow flow;
	
	public IrcMessageFlowListener(Flow flow) {
		this.flow = flow;
	}
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		flow.prepare().data(MutableMemoryData.create()
				.putString("channel", channel)
				.putString("sender", sender)
				.putString("login", login)
				.putString("hostname", hostname)
				.putString("message", message)).run();
	}

	@Override
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
	}

	@Override
	public void onConnect() {
		
	}

}
