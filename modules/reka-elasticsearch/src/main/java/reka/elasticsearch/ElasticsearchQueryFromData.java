package reka.elasticsearch;

import static reka.util.Util.unchecked;

import java.io.IOException;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;

public class ElasticsearchQueryFromData extends ElasticsearchOperation<SearchResponse> {
	
	private final Client client;
	private final String index;
	private final String type;
	private final Path in;
	private final Path out;
	
	public ElasticsearchQueryFromData(Client client, String index, String type, Path in, Path out) {
		this.client = client;
		this.index = index;
		this.type = type;
		this.in = in;
		this.out = out;
	}

	@Override
	public ActionRequestBuilder<?, SearchResponse, ?> request(Data data) {
		return client.prepareSearch(index).setTypes(type).setExtraSource(data.at(in).toJson());
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
