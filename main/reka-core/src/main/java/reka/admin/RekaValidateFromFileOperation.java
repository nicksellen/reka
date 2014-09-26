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
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;
import reka.config.FileSource;
import reka.config.Source;
import reka.config.SourceLinenumbers;
import reka.config.configurer.Configurer.InvalidConfigurationException;
import reka.util.Util;

public class RekaValidateFromFileOperation implements RouterOperation {
	
	public static final RouteKey OK = RouteKey.named("ok");
	public static final RouteKey ERROR = RouteKey.named("error");
	
	private final ApplicationManager manager;
	private final Function<Data,String> filenameFn, identityFn;
	
	public RekaValidateFromFileOperation(ApplicationManager manager, Function<Data,String> filenameFn,  Function<Data,String> identityFn) {
		this.manager = manager;
		this.filenameFn = filenameFn;
		this.identityFn = identityFn;
	}

	@Override
	public void call(MutableData data, RouteCollector router) {
		try {
			String filename = filenameFn.apply(data);
			String identity = identityFn.apply(data);
			File file = new File(filename);
			checkArgument(file.exists(), "file does not exist [%s]", filename);
			checkArgument(!file.isDirectory(), "path is a directory [%s]", filename);
			manager.validate(identity, FileSource.from(file));
			router.routeTo(OK);
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
			router.routeTo(ERROR);
		}
	}
	
}