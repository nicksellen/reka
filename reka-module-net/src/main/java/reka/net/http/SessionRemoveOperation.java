package reka.net.http;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;
import reka.util.Path.Request;

public class SessionRemoveOperation implements Operation {
	
	private final SessionStore store;
	
	private final Path getSessionIdPath = Request.COOKIES.add(HttpSessionsConfigurer.COOKIENAME).add("value");
	
	private final Function<Data,Path> keyFn;
	
	public SessionRemoveOperation(SessionStore store, Function<Data,Path> keyFn) {
		this.store = store;
		this.keyFn = keyFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.getString(getSessionIdPath).ifPresent(id -> {
			Path key = keyFn.apply(data);		
			if (key.isEmpty()) {
				store.remove(id);
			} else {
				store.find(id).ifPresent(sessdata -> {
					synchronized (sessdata) {
						sessdata.remove(key);
					}
				});
			}
		});
	}

}
