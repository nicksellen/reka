package reka.net.http.configurers;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import io.netty.channel.EventLoopGroup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.net.http.operations.NetProxyOperation;

public class NetProxyConfigurer implements OperationConfigurer {
	
	private final EventLoopGroup group;
	
	private final Pattern hostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	private String host;
	private int port;
	
	public NetProxyConfigurer(EventLoopGroup group) {
		this.group = group;
	}

	@Conf.Val
	public NetProxyConfigurer connect(String value) {
		Matcher m = hostAndPort.matcher(value);
		checkConfig(m.matches(), "invalid value, should be in the form host:port");
		host = m.group(1);
		port = Integer.valueOf(m.group(2));
		return this;
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("proxy", () -> new NetProxyOperation(group, host, port));
	}

}
