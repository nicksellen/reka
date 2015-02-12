package reka.nashorn;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("restriction")
public class ThreadLocalNashornRunner implements NashornRunner {
	
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ThreadLocalNashornRunner.class);
	
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
	
	private final ThreadLocal<Bindings> global;
	
	private static void recursivelyFreezeAndSeal(ScriptObjectMirror o) {
		o.freeze();
		o.seal();
		o.values().forEach(v -> {
			if (v instanceof ScriptObjectMirror) {
				recursivelyFreezeAndSeal((ScriptObjectMirror) v);
			}
		});

	}
	
	public ThreadLocalNashornRunner(List<String> initializationScripts) {
		Bindings initBindings = engine.createBindings(); // this creates a new global
		
		try {
			
			for (String initializationScript : initializationScripts) {
				engine.eval(initializationScript, initBindings);
			}
			
			ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();
			
			// we just take the values out and leave the global behind
			initBindings.forEach((k, o) -> {
				if (o instanceof ScriptObjectMirror) {
					recursivelyFreezeAndSeal((ScriptObjectMirror) o);
				}
				builder.put(k, o);
			});
			
			global = new BindingsThreadLocal(builder.build());
			
		} catch (ScriptException e) {
			throw unchecked(e);
		}
	}
	
	public static class BindingsThreadLocal extends ThreadLocal<Bindings> {

		private final Map<String,Object> base;
		
		public BindingsThreadLocal(Map<String,Object> base) {
			this.base = base;
		}

		@Override
		protected Bindings initialValue() {
			Bindings bindings = engine.createBindings(); // each thread needs it's own global
			bindings.putAll(base);
			return bindings;
		}
		
	}
	
	@Override
	public CompiledScript compile(String source) {
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

	@Override
	public Object run(CompiledScript compiledScript, Map<String,Object> data) {
		
		Map<String,Object> map = new HashMap<>();
		Bindings bindings = new SimpleBindings(map);
		bindings.putAll(data);
		Collector collector = new Collector();
		bindings.put("REKA", collector);
		
		try {
			compiledScript.eval(new EvenSimplerScriptContext(global.get(), bindings));
		} catch (ScriptException e) {
			throw unchecked(e);
		}
		
		return collector.result;
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
		//NashornRunner runner = new ThreadLocalNashornRunner(scripts);
		NashornRunner runner = new SingleThreadedNashornRunner(scripts);
		
		/*
		
		System.out.printf("a\n");
		System.out.printf("output: %s\n", runner.run(runner.compile("out.result = a + b; out.boo = { name: 'nick' };"), justOut()));

		System.out.printf("b\n");
		System.out.printf("output: %s\n", runner.run(runner.compile("var a = 3; b = 6; out.result = a + b;"), justOut()));

		System.out.printf("c\n");
		System.out.printf("output: %s\n", runner.run(runner.compile("out.result = a + b; c.woah = 'yeah changed';"), justOut()));
		
		
		ThreadLocalNashornRunner runner2 = new ThreadLocalNashornRunner(scripts);

		System.out.printf("a2\n");
		System.out.printf("output: %s\n", runner2.run(runner2.compile("out.result = a + b; out.woah = c.woah;"), justOut()));
		
		*/
		
		System.out.printf("%s\n", runner.run(runner.compile("data.name = 'nick'; return { name: 'peter', age: 22, data: data, interests: ['running', 'swimming'] };"), justOut()));
	}
	
	private static Map<String,Object> justOut() {
		Map<String,Object> m = new HashMap<>();
		m.put("data", new HashMap<>());
		return m;
	}
	
	public static class Collector {
		public Object result;
		public void collect(ScriptObjectMirror som) {
			result = jsToJava(som).get(NashornRunner.REKA_OUTPUT_PROPERTY);
		}
	}
	
	private static Map<String,Object> jsToJava(ScriptObjectMirror som) {
		if (som.isArray()) throw runtime("sorry can't convert directly from arrays yet");
		return convertMap(som);
	}
	
	private static Map<String,Object> convertMap(ScriptObjectMirror map) {
		Map<String,Object> newmap = new HashMap<>(map.size());
		for (Entry<String, Object> e : map.entrySet()) {
			newmap.put(e.getKey(), convertValue(e.getValue()));
		}
		return newmap;
	}
	
	private static Object convertValue(Object obj) {
		if (obj instanceof ScriptObjectMirror) {
			ScriptObjectMirror som = (ScriptObjectMirror) obj;
			if (som.isArray()) {
				return convertArray(som);
			} else {
				return convertMap(som);
			}
		} else {
			return obj;
		}
	}
	
	private static List<Object> convertArray(ScriptObjectMirror list) {
		List<Object> newlist = new ArrayList<>(list.size());
		for (Object o : list.values()) {
			newlist.add(o);
		}
		return newlist;
	}
	
}

