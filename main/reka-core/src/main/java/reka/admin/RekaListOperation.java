package reka.admin;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.content.Contents.utf8;

import java.util.Map.Entry;
import java.util.Optional;

import reka.Application;
import reka.ApplicationManager;
import reka.api.Path;
import reka.api.Path.PathElements;
import reka.api.content.Contents;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.bundle.PortAndProtocol;

public class RekaListOperation implements SyncOperation {
	
	private final ApplicationManager manager;
	private final Path out;
	
	public RekaListOperation(ApplicationManager manager, Path out) {
		this.manager = manager;
		this.out = out;
	}

	@Override
	public MutableData call(MutableData data) {
		
		for (Entry<String, Application> entry : manager) {
			String id = entry.getKey();
			Application app = entry.getValue();
			MutableData appdata = data.createMapAt(out.add(id));
			appdata.putString("name", app.name().slashes());
			
			MutableData portsdata = appdata.createListAt(path("ports"));
			int i = 0;
			for (PortAndProtocol e : app.ports()) {
				
				Path base = path(PathElements.index(i));
				
				portsdata.put(base.add("port"), Contents.integer(e.port()));
				portsdata.put(base.add("protocol"), Contents.utf8(e.protocol()));
				
				Path details = base.add("details");
				e.details().forEachContent((path, content) -> {
					portsdata.put(details.add(path), content);
				});
				
				Optional<String> host = e.details().getString("host");
				
				if (host.isPresent()) {
					portsdata.putString(base.add("url"), format("%s://%s:%s", e.protocol(), host.get(), e.port()));
				}
				
				i++;
			}
			
			MutableData flowdata = appdata.createListAt(path("flows"));
			for (Path flow : app.flowNames()) {
				flowdata.put(PathElements.nextIndex(), utf8(flow.subpath(app.name().length()).slashes()));
			}
		}
		
		return data;
	}

}
