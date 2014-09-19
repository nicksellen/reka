package reka.jsx;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.script.CompiledScript;

import reka.core.bundle.BundleConfigurer;
import reka.nashorn.NashornRunner;
import reka.nashorn.SingleThreadedNashornRunner;

import com.google.common.io.Resources;

public class JsxBundle implements BundleConfigurer {
	
	private static NashornRunner runner;
	private static CompiledScript jsxCompiler;
	
	private static boolean initialized = false;
	
	public static NashornRunner runner() {
		if (!initialized) initialize();
		return runner;
	}
	
	public static CompiledScript jsxCompiler() {
		if (!initialized) initialize();
		return jsxCompiler;
	}
	
	private static void initialize() {
		try {
			String init = Resources.toString(JsxBundle.class.getResource("/env.js"), StandardCharsets.UTF_8);
			String jsxTransformer = Resources.toString(JsxBundle.class.getResource("/JSXTransformer-0.11.2.js"), StandardCharsets.UTF_8);
			runner = new SingleThreadedNashornRunner(asList(init, jsxTransformer));
			String compiler = Resources.toString(JsxBundle.class.getResource("/compiler.js"), StandardCharsets.UTF_8);
			jsxCompiler = runner.compile(compiler);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	public void setup(BundleSetup bundle) {
		bundle.module(path("jsx"), () -> new JsxModule());
	}

}
