package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.api.Path.PathElements.nextIndex;

import java.io.File;
import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;
import reka.config.FileSource;
import reka.config.Source;
import reka.config.SourceLinenumbers;
import reka.config.configurer.Configurer.InvalidConfigurationException;
import reka.util.Util;

public class RekaValidateFromFileOperation implements RoutingOperation {
	
	private final ApplicationManager manager;
	private final Function<Data,String> filenameFn;
	
	public RekaValidateFromFileOperation(ApplicationManager manager, Function<Data,String> filenameFn) {
		this.manager = manager;
		this.filenameFn = filenameFn;
	}

	@Override
	public MutableData call(MutableData data, RouteCollector router) {
		try {
			String filename = filenameFn.apply(data);
			File file = new File(filename);
			checkArgument(file.exists(), "file does not exist [%s]", filename);
			checkArgument(!file.isDirectory(), "path is a directory [%s]", filename);
			manager.validate(FileSource.from(file));
			router.routeTo("ok");
			return data;
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
		return data;
	}
	
}