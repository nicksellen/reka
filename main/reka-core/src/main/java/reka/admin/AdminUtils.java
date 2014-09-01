package reka.admin;

import reka.Application;
import reka.api.data.MutableData;

public class AdminUtils {

	public static MutableData putAppDetails(MutableData data, Application app) {
		
		data.putString("name", app.name().slashes());
		data.putInt("version", app.version());
		
		app.meta().ifPresent(meta -> data.put("meta", meta));
		
		data.putList("network", list -> {
			
			app.network().forEach(network -> {
				
				list.addMap(m -> {
				
					m.putInt("port", network.port());
					m.putString("protocol", network.protocol());
					
					network.details().forEachContent((path, content) -> {
						m.put(path, content);
					});
					
					network.details().getString("host").ifPresent(host -> {
						StringBuilder sb = new StringBuilder();
						sb.append(network.protocol()).append("://").append(host);
						if (!network.isDefaultPort()) sb.append(':').append(network.port());
						m.putString("url", sb.toString());
					});
				
				});
			
			});
		
		});
		
		data.putList("flows", list -> {
			app.flows().all().forEach(flow -> {
				list.addMap(m -> {
					m.putString("name", flow.name().subpath(app.name().length()).slashes());
				});
			});
		});
		
		return data;
	}
	
}
