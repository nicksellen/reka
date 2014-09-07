package reka.admin;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.api.Path.PathElements.nextIndex;
import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;
import reka.config.Source;
import reka.config.SourceLinenumbers;
import reka.config.StringSource;
import reka.config.configurer.Configurer.InvalidConfigurationException;
import reka.util.Util;

public class RekaValidateFromContentOperation implements RoutingOperation {
	
	private final ApplicationManager manager;
	private final Path in;
	
	public RekaValidateFromContentOperation(ApplicationManager manager, Path in) {
		this.manager = manager;
		this.in = in;
	}

	@Override
	public void call(MutableData data, RouteCollector router) {
		try {
			String configString = RekaModule.getConfigStringFromData(data, in);
			manager.validate(StringSource.from(configString));
			router.routeTo("ok");
			return;
		} catch (Throwable t) {
			t = Util.unwrap(t);
			if (t instanceof InvalidConfigurationException) {
				InvalidConfigurationException e = (InvalidConfigurationException) t;
				e.errors().forEach(error -> {
					data.putMap(path(name("errors"), nextIndex()), map -> {
						Source source = error.config().source();
						SourceLinenumbers linenumbers = source.linenumbers();
						if (linenumbers != null) {
							map.putInt("start-line", linenumbers.startLine());
							map.putInt("end-line", linenumbers.endLine());
							map.putInt("start-pos", linenumbers.startPos());
							map.putInt("end-pos", linenumbers.endPos());
						}
						
					});
				});
			} else {
				data.putString("error", t.getMessage());
			}
			router.routeTo("error");
		}
	}
	
}