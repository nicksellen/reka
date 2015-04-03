package reka.irc;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class IrcSendOperation implements Operation {

	private final RekaBot bot;
	private final Function<Data,String> msgFn;
	
	public IrcSendOperation(RekaBot bot, Function<Data,String> msgFn) {
		this.bot = bot;
		this.msgFn = msgFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		bot.send(msgFn.apply(data));
	}

}
