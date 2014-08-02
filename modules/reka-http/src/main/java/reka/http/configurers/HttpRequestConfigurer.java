package reka.http.configurers;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.core.builder.FlowSegments.async;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.function.Supplier;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.http.operations.HttpRequestOperation;

public class HttpRequestConfigurer implements Supplier<FlowSegment> {
	
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
	public FlowSegment get() {
		return async("http request", () -> new HttpRequestOperation(group, url, out));
	}

}
