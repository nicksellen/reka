package reka.admin;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.content.Contents.utf8;

import java.util.Optional;
import java.util.function.Function;

import reka.Application;
import reka.ApplicationManager;
import reka.api.Path;
import reka.api.Path.PathElements;
import reka.api.content.Contents;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.bundle.NetworkInfo;

public class RekaDetailsOperation implements SyncOperation {
	
	private final ApplicationManager manager;
	private final Path out;
	private final Function<Data,String> idFn;
	
	public RekaDetailsOperation(ApplicationManager manager, Function<Data,String> idFn, Path out) {
		this.manager = manager;
		this.idFn = idFn;
		this.out = out;
	}

	@Override
	public MutableData call(MutableData data) {
		
		String identity = idFn.apply(data);
		Optional<Application> opt = manager.get(identity);
		
		if (!opt.isPresent()) return data;
		
		Application app = opt.get();
		
		MutableData appdata = data.createMapAt(out);
		
		appdata.putString("name", app.name().slashes());
		
		if (app.meta().isPresent()) {
			appdata.put(path("meta"), app.meta());
		}
		
		//appdata.putBool("redeployable", manager.hasSourceFor(identity));
		//appdata.putBool("removable", manager.hasSourceFor(identity));
		
		MutableData portsdata = appdata.createListAt(path("network"));
		int i = 0;
		for (NetworkInfo e : app.network()) {
			
			Path base = path(PathElements.index(i));
			
			portsdata.put(base.add("port"), Contents.integer(e.port()));
			portsdata.put(base.add("protocol"), Contents.utf8(e.protocol()));
			
			e.details().forEachContent((path, content) -> {
				portsdata.put(base.add(path), content);
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
		
		return data;
	}

}
