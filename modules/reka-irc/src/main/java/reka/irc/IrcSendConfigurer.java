package reka.irc;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class IrcSendConfigurer implements OperationConfigurer {

	private Function<Data,String> msgFn;
	
	@Conf.Val
	public void msg(String val) {
		msgFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("send", ctx -> new IrcSendOperation(ctx.get(IrcConfigurer.BOT), msgFn));
	}

}
