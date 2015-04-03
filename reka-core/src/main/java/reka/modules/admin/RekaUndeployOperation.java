package reka.modules.admin;

import static reka.api.Path.dots;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.app.manager.ApplicationManager;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.dirs.AppDirs;
import reka.util.dirs.BaseDirs;

public class RekaUndeployOperation implements Operation {
	
	private final ApplicationManager manager;
	private final BaseDirs basedirs;
	private final Function<Data,Path> appPathFn;
	
	public RekaUndeployOperation(ApplicationManager manager, AppDirs dirs, Function<Data,Path> appPathFn) {
		this.manager = manager;
		this.basedirs = dirs.basedirs();
		this.appPathFn = appPathFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		
		
		Path appPath = appPathFn.apply(data);
		basedirs.delete(appPath);
		manager.undeploy(appPath);
	}
	
	public static Logger log = LoggerFactory.getLogger(RekaUndeployOperation.class);
	
	public static void main(String[] args) {

		MutableData data = MutableMemoryData.create();

		data.putMap(dots("things.in.here"), map -> {
			map.putString("name", "Nick");
			map.putMap(dots("more.things.inside.here"), map2 -> {
				map2.putString("whatever", "yay");
			});
		});
		
		data.at(dots("things.in.here")).forEachContent((path, content) -> {
			System.out.printf("found %s -> %s\n", path.dots(), content);
		});
	}
	
}