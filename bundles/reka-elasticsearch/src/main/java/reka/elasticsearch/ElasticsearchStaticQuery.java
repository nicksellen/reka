package reka.elasticsearch;

import static reka.util.Util.unchecked;

import java.io.IOException;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;

public class ElasticsearchStaticQuery extends ElasticsearchOperation<SearchResponse> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Client client;
	private final String index;
	private final String type;
	private final String query;
	private final Path out;
	
	public ElasticsearchStaticQuery(Client client, String index, String type, String query, Path out) {
		log.debug("creating es query with client: {}", client);
		this.client = client;
		this.index = index;
		this.type = type;
		this.query = query;
		this.out = out;
	}

	@Override
	public ActionRequestBuilder<?, SearchResponse, ?> request(Data data) {
		return client.prepareSearch(index).setTypes(type).setExtraSource(query);
	}

	@Override
	public MutableData response(MutableData data, SearchResponse response) {
		
		DataXContent x = new DataXContent(data.createMapAt(out));
		
		try {
			XContentBuilder builder = new XContentBuilder(x, null);
			builder.startObject();
			response.toXContent(builder, ToXContent.EMPTY_PARAMS);
			builder.endObject();
		} catch (IOException e) {
			throw unchecked(e);
		}
		
		return data;
	}

}
