package reka.nashorn;

import static reka.util.Util.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.core.data.memory.MutableMemoryData;

public class NashornRunOperation implements Operation {

	private final NashornRunner runner;
	private final CompiledScript compiled;
	private final Path out;
	
	public NashornRunOperation(NashornRunner runner, String scriptSource, Path out) {
		this.runner = runner;
		compiled = runner.compile(scriptSource);
		this.out = out;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public MutableData call(MutableData data) {
		Map<String,Object> m = new HashMap<>();
		m.put("data", data.viewAsMap());
		m.put("out", new HashMap<>());
		Object outval = runner.run(compiled, m).get("out");
		if (outval instanceof Map) {
			MutableMemoryData.createFromMap((Map<String,Object>) outval).forEachContent((path, content) -> 
				data.put(out.add(path), content));
		} else if (outval instanceof String) {
			data.putString(out, (String) outval);
		} else {
			throw runtime("not sure what to do with %s", outval);
		}
		return data;
	}

}
