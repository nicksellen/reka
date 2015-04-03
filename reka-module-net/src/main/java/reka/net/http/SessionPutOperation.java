package reka.net.http;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;
import reka.util.Path.Request;
import reka.util.Path.Response;

public class SessionPutOperation implements Operation {
	
	private final SessionStore store;
	
	private final Path getSessionIdPath = Request.COOKIES.add(HttpSessionsConfigurer.COOKIENAME).add("value");
	private final Path setSessionIdPath = Response.COOKIES.add(HttpSessionsConfigurer.COOKIENAME).add("value");
	
	private final Function<Data,Path> keyFn;
	private final Function<Data,String> valFn;
	
	public SessionPutOperation(SessionStore store, Function<Data,Path> keyFn, Function<Data,String> valFn) {
		this.store = store;
		this.keyFn = keyFn;
		this.valFn = valFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		Optional<String> o = data.getString(getSessionIdPath);
		String id;
		if (o.isPresent()) {
			id = o.get();
		} else {
			id = UUID.randomUUID().toString();
			data.putString(setSessionIdPath, id);
		}
		MutableData sessdata = store.findOrCreate(id);
		
		synchronized (sessdata) {
			sessdata.putString(keyFn.apply(data), valFn.apply(data));	
		}
		
	}

}
