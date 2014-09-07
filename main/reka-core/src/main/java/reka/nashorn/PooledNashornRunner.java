package reka.nashorn;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("restriction")
public class PooledNashornRunner implements NashornRunner {
	
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
	
	private final ObjectPool<Bindings> pool;
	
	public PooledNashornRunner(List<String> initializationScripts) {
		Bindings globalBindings = engine.createBindings(); // this creates a new global
		
		try {
			
			for (String initializationScript : initializationScripts) {
				engine.eval(initializationScript, globalBindings);
			}
			
			ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();
			
			globalBindings.forEach((k, o) -> {
				if (o instanceof ScriptObjectMirror) {
					ScriptObjectMirror som = (ScriptObjectMirror) o;
					som.freeze();
					som.seal();
				}
				builder.put(k, o);
			});
			
			GenericObjectPoolConfig config = new GenericObjectPoolConfig();
			config.setMinIdle(4);
			pool = new GenericObjectPool<>(new BindingsFactory(builder.build()), config);
			
		} catch (ScriptException e) {
			throw unchecked(e);
		}
	}
	
	public static class BindingsFactory extends BasePooledObjectFactory<Bindings> {

		private final Map<String,Object> base;
		
		public BindingsFactory(Map<String,Object> base) {
			this.base = base;
		}
		
		@Override
		public Bindings create() throws Exception {
			Bindings bindings = engine.createBindings(); // each thread needs it's own global
			bindings.putAll(base);
			return bindings;
		}

		@Override
		public PooledObject<Bindings> wrap(Bindings obj) {
			return new DefaultPooledObject<>(obj);
		}
		
	}
	
	@Override
	public CompiledScript compile(String source) {
		try {
			return compiler.compile(new StringBuilder()
				.append("(function(){\n")
					.append(source)
				.append("\n}).call(null)").toString());
		} catch (ScriptException e) {
			throw unchecked(e);
		}
	}

	@Override
	public Map<String,Object> run(CompiledScript compiledScript, Map<String,Object> data) {
		
		Map<String,Object> map = new HashMap<>();
		Bindings bindings = new SimpleBindings(map);
		bindings.putAll(data);
		
		try {
			Bindings global = pool.borrowObject();
			try {
				compiledScript.eval(new EvenSimplerScriptContext(global, bindings));
			} finally {
				pool.returnObject(global);
			}
		} catch (Exception e) {
			throw unchecked(e);
		}
		
		return map;
	}
	
	public static class EvenSimplerScriptContext extends SimpleScriptContext {
		
		public EvenSimplerScriptContext(Bindings engineScope, Bindings globalBindings) {
			this.engineScope = engineScope;
			this.globalScope = globalBindings;
		}
		
	}
	
}
