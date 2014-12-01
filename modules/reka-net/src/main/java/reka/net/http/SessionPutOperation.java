package reka.net.http;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

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
	public void call(MutableData data) {
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
