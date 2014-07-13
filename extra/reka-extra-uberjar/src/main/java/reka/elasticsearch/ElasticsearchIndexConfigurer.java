package reka.elasticsearch;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.async;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.elasticsearch.client.Client;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class ElasticsearchIndexConfigurer implements Supplier<FlowSegment> {
	
	private final AtomicReference<Client> clientRef;
	
	private Function<Data,String> indexFn, typeFn, idFn;
	private Function<Data,Data> contentFn;
	
	public ElasticsearchIndexConfigurer(AtomicReference<Client> clientRef) {
		this.clientRef = clientRef;
	}
	
	@Conf.At("index")
	public void index(String val) {
		indexFn = StringWithVars.compile(val);
	}
	
	@Conf.At("type")
	public void type(String val) {
		typeFn = StringWithVars.compile(val);
	}
	
	@Conf.At("id")
	public void id(String val) {
		idFn = StringWithVars.compile(val);
	}
	
	@Conf.At("content")
	public void content(String val) {
		Function<Data,Path> contentPathFn = StringWithVars.compile(val).andThen(s -> dots(s));
		contentFn = (data) -> data.at(contentPathFn.apply(data));
	}

	@Override
	public FlowSegment get() {
		return async("index", () -> new ElasticsearchIndex(clientRef.get(), indexFn, typeFn, idFn, contentFn));
	}

}
