package reka.net.http;

import java.util.function.Function;

import reka.api.Path;
import reka.api.Path.Request;
import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class SessionGetOperation implements Operation {
	
	private final SessionStore store;
	private final Path sessionIdPath = Request.COOKIES.add(HttpSessionsConfigurer.COOKIENAME).add("value");
	private final Function<Data,Path> keyFn;
	
	public SessionGetOperation(SessionStore store, Function<Data,Path> keyFn) {
		this.store = store;
		this.keyFn = keyFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		
		data.getString(sessionIdPath).ifPresent(sessionid -> {
			store.find(sessionid).ifPresent(sessdata -> {
				synchronized (sessdata) {
					Path key = keyFn.apply(data);
					sessdata.at(key).forEachContent((path, content) -> data.put(key.add(path), content));	
				}
			});
		});
		
	}

}
