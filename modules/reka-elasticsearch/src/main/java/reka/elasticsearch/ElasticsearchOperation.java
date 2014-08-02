package reka.elasticsearch;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class ElasticsearchOperation <Response extends ActionResponse> implements AsyncOperation {
	
	public abstract ActionRequestBuilder<?,Response,?> request(Data data);
	public abstract MutableData response(MutableData data, Response response);
	
	@Override
	public ListenableFuture<MutableData> call(MutableData data) {
		ListenableFuture<Response> lf = new ListenableElasticsearchFuture<>(request(data.immutable()).execute());
		return Futures.transform(lf, new com.google.common.base.Function<Response,MutableData>(){

			@Override
			public MutableData apply(Response obj) {
				return response(data, obj);
			}
			
		});
	}

}
