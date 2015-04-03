package reka.net.http.configurers;

import static reka.util.Path.dots;
import static reka.util.Path.path;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.net.http.operations.HttpRequestOperation;
import reka.util.Path;

public class HttpRequestConfigurer implements OperationConfigurer {
	
	private final EventLoopGroup group;
	private final Class<? extends Channel> channelType;
	
	private String url;
	private Path into = path("response");
	
	public HttpRequestConfigurer(EventLoopGroup group, Class<? extends Channel> channelType) {
		this.group = group;
		this.channelType = channelType;
	}

	@Conf.Val
	@Conf.At("url")
	public HttpRequestConfigurer url(String value) {
		url = value;
		return this;
	}
	
	@Conf.At("into")
	public void out(String val) {
		into = dots(val);
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("request", () -> new HttpRequestOperation(group, channelType, url, into));
	}

}
