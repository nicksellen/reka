package reka.irc;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class IrcSendConfigurer implements OperationConfigurer {

	private Function<Data,String> msgFn;
	
	@Conf.Val
	public void msg(String val) {
		msgFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("send", () -> new IrcSendOperation(ops.ctx().get(IrcConfigurer.BOT), msgFn));
	}

}
