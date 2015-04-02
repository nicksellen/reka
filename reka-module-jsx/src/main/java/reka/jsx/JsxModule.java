package reka.jsx;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.script.CompiledScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.nashorn.NashornRunner;
import reka.nashorn.SingleThreadedNashornRunner;

import com.google.common.io.Resources;

public class JsxModule implements Module {
	
	@Override
	public Path base() {
		return path("jsx");
	}
	
	private static final Object lock = new Object();
	
	private static final Logger log = LoggerFactory.getLogger(JsxModule.class);
	
	private static volatile NashornRunner runner;
	private static volatile CompiledScript jsxCompiler;
	
	private static volatile boolean initialized = false;
	
	public static NashornRunner runner() {
		if (!initialized) initialize();
		return runner;
	}
	
	public static CompiledScript jsxCompiler() {
		if (!initialized) initialize();
		return jsxCompiler;
	}
	
	private static void initialize() {
		synchronized (lock) {
			if (initialized) return;
			try {
				String version = "0.11.2";
				log.info("initializing jsx engine ({})", version);
				String init = Resources.toString(JsxModule.class.getResource("/env.js"), StandardCharsets.UTF_8);
				String jsxTransformer = Resources.toString(JsxModule.class.getResource("/JSXTransformer-" + version + ".js"), StandardCharsets.UTF_8);
				runner = new SingleThreadedNashornRunner(asList(init, jsxTransformer));
				String compiler = Resources.toString(JsxModule.class.getResource("/compiler.js"), StandardCharsets.UTF_8);
				jsxCompiler = runner.compile(compiler);
				initialized = true;
			} catch (IOException e) {
				throw unchecked(e);
			}
		}
	}

	public void setup(ModuleDefinition module) {
		module.main(() -> new JsxConfigurer());
	}

}
