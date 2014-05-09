package reka.nashorn;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class NashornRunner {

	private static final ScriptEngineManager manager = new ScriptEngineManager();
	private static ScriptEngineFactory factoryFor(String engineName) {
		for (ScriptEngineFactory factory : manager.getEngineFactories()) {
			if (factory.getNames().contains(engineName)) return factory;
		}
		throw runtime("couldn't find script engine for %s", engineName);
	}
	
	private static final NashornScriptEngineFactory factory = (NashornScriptEngineFactory) factoryFor("nashorn");

	private final NashornScriptEngine engine;
	private final Compilable compiler;
	
	private final jdk.nashorn.internal.objects.Global global;
	
	public NashornRunner(List<String> initializationScripts) {
		String[] options;
		options = new String[] { "--global-per-engine" };
		engine = (NashornScriptEngine) factory.getScriptEngine(options);
		compiler = (Compilable) engine;
		
		try {
			
			for (String initializationScript : initializationScripts) {
				engine.eval(initializationScript);
			}
			
		} catch (ScriptException e) {
			throw unchecked(e);
		}
		
		try {
			Field f = engine.getClass().getDeclaredField("global");
			f.setAccessible(true);
			global = (jdk.nashorn.internal.objects.Global) f.get(engine);
			//g.seal();
			global.freeze(); // prevents any changes to global
			
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw unchecked(e);
		}
	}
	
	public CompiledScript compile(String source) {
		try {
			return compiler.compile(new StringBuilder()
				.append("(function(){\n")
					.append(source)
				.append("\n})()").toString());
		} catch (ScriptException e) {
			throw unchecked(e);
		}
	}
	
	public Map<String,Object> run(CompiledScript compiledScript, Map<String,Object> data) {
		
		ScriptContext context = new SimpleScriptContext();
		Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
		
		data.forEach((k, v) -> {
			bindings.put(k, v);
		});
		
		try {
			compiledScript.eval(context);
		} catch (ScriptException e) {
			throw unchecked(e);
		}
		
		Map<String,Object> result = new HashMap<>();
		
		bindings.forEach((k, v) -> {
			result.put(k, v);
		});
		
		return result;
	}
	
}

