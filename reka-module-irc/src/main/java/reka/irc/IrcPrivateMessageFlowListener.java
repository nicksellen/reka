package reka.irc;

import reka.api.flow.Flow;
import reka.core.data.memory.MutableMemoryData;
import reka.irc.RekaBot.IrcListener;

public class IrcPrivateMessageFlowListener implements IrcListener {

	private final Flow flow;
	
	public IrcPrivateMessageFlowListener(Flow flow) {
		this.flow = flow;
	}
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
	}

	@Override
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		flow.prepare().data(MutableMemoryData.create()
				.putString("sender", sender)
				.putString("login", login)
				.putString("hostname", hostname)
				.putString("message", message)).run();
	}

	@Override
	public void onConnect() {
		
	}

}
