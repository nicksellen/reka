package reka.nashorn;

import static reka.api.Path.path;

import java.util.List;

import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

// http://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/intro.html#sthref14

// http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/

// threading: https://blogs.oracle.com/nashorn/entry/nashorn_multi_threading_and_mt

public class NashornModule implements Module {

	@Override
	public Path base() {
		return path("nashorn");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new NashornConfigurer());
	}
	
	public NashornRunner createSingleThreadedRunner(List<String> initializationScripts) {
		return new SingleThreadedNashornRunner(initializationScripts);
	}
	
	public NashornRunner createThreadLocalRunner(List<String> initializationScripts) {
		return new ThreadLocalNashornRunner(initializationScripts);
	}

}
