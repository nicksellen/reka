package reka.nashorn;

import static reka.util.Util.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
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
	public void call(MutableData data, OperationContext ctx) {
		Map<String,Object> m = new HashMap<>();
		m.put("data", data.viewAsMap());
		Object outval = runner.run(compiled, m);
		
		if (outval instanceof Map) {
			MutableMemoryData.createFromMap((Map<String,Object>) outval).forEachContent((path, content) -> data.put(out.add(path), content));
		} else if (outval instanceof List) {
			MutableMemoryData.createFromList((List<Object>) outval).forEachContent((path, content) -> 
				data.put(out.add(path), content));
		} else if (outval instanceof String) {
			data.putString(out, (String) outval);
		} else if (outval instanceof Integer) {
			data.putInt(out, (Integer) outval);
		} else if (outval instanceof Long) {
			data.putLong(out, (Long) outval);
		} else if (outval instanceof Double) {
			data.putDouble(out, (Double) outval);
		} else if (outval instanceof Boolean) {
			data.putBool(out, (Boolean) outval);
		} else if (outval == null) {
			// ignore
		} else {
			throw runtime("not sure what to do with %s (%s)", outval, outval.getClass());
		}
	}

}
