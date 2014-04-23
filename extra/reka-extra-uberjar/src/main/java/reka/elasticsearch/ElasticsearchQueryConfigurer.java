package reka.elasticsearch;

import static java.util.Arrays.asList;
import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.async;
import static reka.core.builder.FlowSegments.label;
import static reka.core.builder.FlowSegments.seq;
import static reka.core.builder.FlowSegments.sync;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.javascript.Javascript;

public class ElasticsearchQueryConfigurer implements Supplier<FlowSegment> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicReference<Client> clientRef;
	
	private String query, index, type;
	private String js;
	private Path out = dots("elasticsearch");
	
	public ElasticsearchQueryConfigurer(AtomicReference<Client> clientRef) {
		this.clientRef = clientRef;
	}
	
	@Conf.At("query")
	public void query(Config config) {
		if (config.hasDocument()) {
			log.debug("building es query with doc tye [{}]", config.documentType());
			if (asList("js", "javascript").contains(config.documentType())) {
				js = config.documentContentAsString();
			} else {
				query = config.documentContentAsString();
			}
			if (config.hasValue()) {
				out = dots(config.valueAsString());
			}
		} else if (config.hasValue()) {
			query = config.valueAsString();
		}
	}
	
	@Conf.At("index")
	public void index(String val) {
		index = val;
	}
	
	@Conf.At("type")
	public void type(String val) {
		type = val;
	}
	
	@Conf.At("out")
	public void out(String val) {
		out = dots(val);
	}
	
	@Override
	public FlowSegment get() {
		if (js != null) {
			Path queryPath = dots("elasticsearch.query");
			return label("query", seq(
						sync("build", () -> 
							new Javascript(js, queryPath)), 
						async("run", () -> 
							new ElasticsearchQueryFromData(clientRef.get(), index, type, queryPath, out))));
		} else {

			return async("query", () -> new ElasticsearchStaticQuery(clientRef.get(), index, type, query, out));
		}
	}

}
