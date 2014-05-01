package reka.nashorn;

import static reka.util.Util.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.data.memory.MutableMemoryData;

public class NashornRunOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ThreadLocal<NashornRunner> runner;
	private final Path out;
	
	public NashornRunOperation(List<String> initializationScripts, String scriptSource, Path out) {
		runner = new ThreadLocal<NashornRunner>(){

			@Override
			protected NashornRunner initialValue() {
				return new NashornRunner(initializationScripts, scriptSource);
			}
		};
		this.out = out;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public MutableData call(MutableData data) {
		
		Map<String,Object> m = new HashMap<>();
		m.put("data", data.toMap());
		m.put("out", new HashMap<>());
		
		Object outval = runner.get().run(m).get("out");
		if (outval instanceof Map) {
			Data outdata = MutableMemoryData.createFromMap((Map<String,Object>) outval);
			data.put(out, outdata);
		} else if (outval instanceof String) {
			data.putString(out, (String) outval);
		} else {
			throw runtime("not sure what to do with %s", outval);
		}
		
		return data;
		
	}

}
