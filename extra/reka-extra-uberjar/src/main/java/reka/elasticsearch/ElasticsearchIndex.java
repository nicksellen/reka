package reka.elasticsearch;

import java.util.function.Function;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.data.MutableData;

public class ElasticsearchIndex extends ElasticsearchOperation<IndexResponse> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Client client;
	private final Function<Data,String> indexFn, typeFn, idFn;
	private final Function<Data,Data> contentFn;
	
	public ElasticsearchIndex(Client client, 
			Function<Data,String> indexFn, 
			Function<Data,String> typeFn, 
			Function<Data,String> idFn, 
			Function<Data,Data> contentFn) {
		this.client = client;
		this.indexFn = indexFn;
		this.typeFn = typeFn;
		this.idFn = idFn;
		this.contentFn = contentFn;
	}
	
	@Override
	public ActionRequestBuilder<?, IndexResponse, ?> request(Data data) {
		Data content = contentFn.apply(data);
		
		IndexRequestBuilder req = client.prepareIndex();
		
		req.setIndex(indexFn.apply(data));
		req.setType(typeFn.apply(data));
		if (idFn != null) {
			req.setId(idFn.apply(data));
		}
		
		log.debug("indexing [{}]", content.toPrettyJson());
		if (content.isContent()) {
			String source = content.content().asUTF8();
			log.debug("  as [{}]", source);
			req.setSource(source);
		} else {
			String source = content.toJson();
			log.debug(" as [{}]", source);
			req.setSource(source);
		}
		
		return req;
	}

	@Override
	public MutableData response(MutableData data, IndexResponse response) {
		return data;
	}

}
