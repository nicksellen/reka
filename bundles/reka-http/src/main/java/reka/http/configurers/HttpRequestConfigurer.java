package reka.http.configurers;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import io.netty.channel.nio.NioEventLoopGroup;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.http.operations.HttpRequestOperation;
import reka.nashorn.OperationsConfigurer;

public class HttpRequestConfigurer implements OperationsConfigurer {
	
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
