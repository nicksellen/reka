package reka.elasticsearch;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;

public class ElasticsearchIndex extends ElasticsearchOperation<IndexResponse> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Client client;
	private final String index, type;
	private final Path in;
	
	public ElasticsearchIndex(Client client, String index, String type, Path in) {
		this.client = client;
		this.index = index;
		this.type = type;
		this.in = in;
	}
	
	@Override
	public ActionRequestBuilder<?, IndexResponse, ?> request(Data data) {
		Data val = data.at(in);
		log.debug("indexing [{}]", val.toPrettyJson());
		if (val.isContent()) {
			String source = val.content().asUTF8();
			log.debug("  as [{}]", source);
			return client.prepareIndex(index, type).setSource(source);
		} else {
			String source = val.toJson();
			log.debug(" as [{}]", source);
			return client.prepareIndex(index, type).setSource(source);
		}
	}

	@Override
	public MutableData response(MutableData data, IndexResponse response) {
		return data;
	}

}
