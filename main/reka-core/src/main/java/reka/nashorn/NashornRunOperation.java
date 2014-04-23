package reka.nashorn;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.data.memory.MutableMemoryData;

public class NashornRunOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ScriptEngine engine;
	private final CompiledScript script;
	private final Path out;
	//private final ScriptContext engineContext;
	
	public NashornRunOperation(ScriptEngine engine, String script, Path out) {
		this.engine = engine;
		//this.engineContext = engine.getContext();
		this.out = out;
		try {
			this.script = ((Compilable) engine).compile(script);
		} catch (ScriptException e) {
			throw unchecked(e);
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public MutableData call(MutableData data) {
		try {
			
			// FIXME: hmmmm this kind of works but it crazy slow.
			// need a cheap way to create isolated contexts. I had it down in Rhino :(
			// I think I need to open up the Nashorn API a bit more...
			
			ScriptContext context = new SimpleScriptContext();
			ScriptContext engineContext = engine.getContext();
			
			
			context.setBindings(engineContext.getBindings(ScriptContext.ENGINE_SCOPE), ScriptContext.GLOBAL_SCOPE);
			context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
			
			//context.setBindings(engineContext.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
			//context.setBindings(engineContext.getBindings(ScriptContext.ENGINE_SCOPE), ScriptContext.ENGINE_SCOPE);
			
			Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
			

			
			/*
			ScriptContext context = engine.getContext();
			Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
			*/
			
			bindings.put("out", new HashMap<String,Object>());
			
			script.eval(context);
			
			Object outval = bindings.get("out");
			if (outval instanceof Map) {
				Data outdata = MutableMemoryData.createFromMap((Map<String,Object>) outval);
				data.put(out, outdata);
			} else if (outval instanceof String) {
				data.putString(out, (String) outval);
			} else {
				throw runtime("not sure what to do with %s", outval);
			}
			return data;
		} catch (ScriptException e) {
			throw unchecked(e);
		}
	}

}
