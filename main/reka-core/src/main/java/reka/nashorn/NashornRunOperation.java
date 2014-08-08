package reka.nashorn;

import static reka.util.Util.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.data.memory.MutableMemoryData;

public class NashornRunOperation implements SyncOperation {
	
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ThreadLocal<NashornRunner> runner;
	private final ThreadLocal<CompiledScript> compiled;
	private final Path out;
	
	public NashornRunOperation(ThreadLocal<NashornRunner> runner, String scriptSource, Path out) {
		
		this.runner = runner; 
		
		compiled = new ThreadLocal<CompiledScript>(){
			@Override
			protected CompiledScript initialValue() {
				return runner.get().compile(scriptSource);
			}
		};
		
		this.out = out;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public MutableData call(MutableData data) {
		
		Map<String,Object> m = new HashMap<>();
		m.put("data", data.viewAsMap());
		m.put("out", new HashMap<>());
		
		Object outval = runner.get().run(compiled.get(), m).get("out");
		if (outval instanceof Map) {
			Data outdata = MutableMemoryData.createFromMap((Map<String,Object>) outval);
			outdata.forEachContent((path, content) -> {
				data.put(out.add(path), content);
			});
		} else if (outval instanceof String) {
			data.putString(out, (String) outval);
		} else {
			throw runtime("not sure what to do with %s", outval);
		}
		
		return data;
		
	}

}
