package reka.admin;

import java.util.List;
import java.util.Optional;

import reka.Application;
import reka.ApplicationManager;
import reka.api.data.MutableData;
import reka.core.runtime.NoFlowVisualizer;
import reka.core.setup.StatusReport;

public class AdminUtils {

	public static MutableData putAppDetails(MutableData data, Application app, Optional<List<StatusReport>> statusMaybe) {
		
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
					m.putString("name", flow.name().slashes());
				});
			});
			if (!(app.initializerVisualizer() instanceof NoFlowVisualizer)) {
				list.addMap(m -> {
					m.putString("name", ApplicationManager.INITIALIZER_VISUALIZER_NAME.slashes());
				});
			}
		});
		statusMaybe.ifPresent(status -> {
			data.putList("status", list -> {
				status.forEach(statusItem -> {
					list.addMap(report -> {
						statusItem.data().forEachContent((path, content) -> report.put(path, content));
						report.putBool("up", statusItem.up());
						report.putString("version", statusItem.version());
						report.putString("module", statusItem.name());
					});
				});
			});
		});
		
		return data;
	}
	
}
