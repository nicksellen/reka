package reka.nashorn;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import reka.nashorn.ThreadLocalNashornRunner.Collector;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("restriction")
public class SingleThreadedNashornRunner implements NashornRunner {

	private static final ScriptEngineManager manager = new ScriptEngineManager();
	private static ScriptEngineFactory factoryFor(String engineName) {
		for (ScriptEngineFactory factory : manager.getEngineFactories()) {
			if (factory.getNames().contains(engineName)) return factory;
		}
		throw runtime("couldn't find script engine for %s", engineName);
	}
	
	private static final NashornScriptEngineFactory factory = (NashornScriptEngineFactory) factoryFor("nashorn");

	private static final NashornScriptEngine engine;
	private static final Compilable compiler;
	
	static {
		engine = (NashornScriptEngine) factory.getScriptEngine();
		compiler = (Compilable) engine;
	}
	
	private final Bindings global;
	private final Object lock = new Object();
	
	public SingleThreadedNashornRunner(List<String> initializationScripts) {
		Bindings initBindings = engine.createBindings(); // this creates a new global
		
		try {
			
			for (String initializationScript : initializationScripts) {
				engine.eval(initializationScript, initBindings);
			}
			
			ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();
			
			// we just take the values out and leave the global behind
			initBindings.forEach((k, o) -> {
				if (o instanceof ScriptObjectMirror) {
					ScriptObjectMirror som = (ScriptObjectMirror) o;
					som.freeze();
					som.seal();
				}
				builder.put(k, o);
			});
			
			Bindings bindings = engine.createBindings(); // each thread needs it's own global
			bindings.putAll(builder.build());
			global = bindings;
			
		} catch (ScriptException e) {
			throw unchecked(e);
		}
	}
	
	@Override
	public CompiledScript compile(String source) {
		synchronized (lock) {
			try {
				return compiler.compile(new StringBuilder()
					.append("REKA.collect({ ")
						.append(REKA_OUTPUT_PROPERTY)
						.append(": (function(){\n")
							.append(source)
						.append("\n}).call(null)\n")
					.append("});").toString());
			} catch (ScriptException e) {
				throw unchecked(e);
			}
		}
	}

	@Override
	public Object run(CompiledScript compiledScript, Map<String,Object> data) {
		synchronized (lock) {
			Collector collector = new Collector();
			data.put("REKA", collector);
			Bindings bindings = new SimpleBindings(NashornDataWrapper.wrapMap(data));
			try {
				compiledScript.eval(new EvenSimplerScriptContext(global, bindings));
			} catch (ScriptException e) {
				throw unchecked(e);
			}
			return collector.result;
		}
	}
	
	public static class EvenSimplerScriptContext extends SimpleScriptContext {
		
		public EvenSimplerScriptContext(Bindings engineScope, Bindings globalBindings) {
			this.engineScope = engineScope;
			this.globalScope = globalBindings;
		}
		
	}

	public static void main(String[] args) {
		List<String> scripts = new ArrayList<>();
		scripts.add("a = 10");
		scripts.add("b = 5");
		scripts.add("c = { woah: 'yeah' }");
		scripts.add("d = function(){}");
		SingleThreadedNashornRunner runner = new SingleThreadedNashornRunner(scripts);
		
		System.out.printf("a\n");
		System.out.printf("output: %s\n", runner.run(runner.compile("out.result = a + b;"), justOut()));

		System.out.printf("b\n");
		System.out.printf("output: %s\n", runner.run(runner.compile("var a = 3; b = 6; out.result = a + b;"), justOut()));

		System.out.printf("c\n");
		System.out.printf("output: %s\n", runner.run(runner.compile("out.result = a + b;"), justOut()));
		

		SingleThreadedNashornRunner runner2 = new SingleThreadedNashornRunner(scripts);

		System.out.printf("a2\n");
		System.out.printf("output: %s\n", runner2.run(runner2.compile("out.result = a + b;"), justOut()));
	}
	
	private static Map<String,Object> justOut() {
		Map<String,Object> m = new HashMap<>();
		m.put("out", new HashMap<>());
		return m;
	}
	
}

