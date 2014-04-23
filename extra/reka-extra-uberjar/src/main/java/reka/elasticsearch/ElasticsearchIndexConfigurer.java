package reka.elasticsearch;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.async;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.elasticsearch.client.Client;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;

public class ElasticsearchIndexConfigurer implements Supplier<FlowSegment> {
	
	private final AtomicReference<Client> clientRef;
	
	private String index, type;
	private Path in;
	
	public ElasticsearchIndexConfigurer(AtomicReference<Client> clientRef) {
		this.clientRef = clientRef;
	}
	
	@Conf.At("index")
	public void index(String val) {
		index = val;
	}
	
	@Conf.At("type")
	public void type(String val) {
		type = val;
	}
	
	@Conf.At("in")
	public void in(String val) {
		in = dots(val);
	}

	@Override
	public FlowSegment get() {
		return async("index", () -> new ElasticsearchIndex(clientRef.get(), index, type, in));
	}

}
