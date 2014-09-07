package reka.http.configurers;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import io.netty.channel.nio.NioEventLoopGroup;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.http.operations.HttpRequestOperation;
import reka.nashorn.OperationConfigurer;

public class HttpRequestConfigurer implements OperationConfigurer {
	
	private final NioEventLoopGroup group;
	
	private String url;
	private Path out = path("response");
	
	public HttpRequestConfigurer(NioEventLoopGroup group) {
		this.group = group;
	}

	@Conf.Val
	@Conf.At("url")
	public HttpRequestConfigurer url(String value) {
		url = value;
		return this;
	}
	
	@Conf.At("out")
	public void out(String val) {
		out = dots(val);
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("http request", store -> new HttpRequestOperation(group, url, out));
	}

}
