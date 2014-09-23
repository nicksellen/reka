package reka.admin;

import static java.util.Comparator.comparing;

import java.util.List;

import reka.Application;
import reka.ApplicationManager;
import reka.api.data.MutableData;
import reka.core.runtime.NoFlowVisualizer;
import reka.core.setup.StatusProvider.StatusReport;

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
			if (!(app.initializerVisualizer() instanceof NoFlowVisualizer)) {
				list.addMap(m -> {
					m.putString("name", ApplicationManager.INITIALIZER_VISUALIZER_NAME.slashes());
				});
			}
		});
		
		data.putList("status", list -> {
			List<StatusReport> statuses = app.status();
			statuses.sort(comparing(StatusReport::name));
			statuses.forEach(status -> {
				list.addMap(report -> {
					status.data().forEachContent((path, content) -> report.put(path, content));
					report.putBool("up", status.up());
					report.putString("name", status.name());
				});
			});
		});
		
		return data;
	}
	
}
