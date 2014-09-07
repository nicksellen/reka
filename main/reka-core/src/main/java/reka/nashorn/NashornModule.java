package reka.nashorn;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import reka.api.IdentityKey;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationSetup;
import reka.builtins.BuiltinsModule.PutDataOperation;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class NashornModule extends ModuleConfigurer {

	protected static final IdentityKey<NashornRunner> RUNNER = IdentityKey.named("nashorn runner");
	
	private final List<String> scripts = new ArrayList<>();
	private final Map<String,String> ops = new HashMap<>();
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			scripts.add(config.documentContentAsString());
		}
	}
	
	@Conf.Each("script")
	public void script(Config config) {
		checkConfig(config.hasDocument(), "must have document");
		scripts.add(config.documentContentAsString());
	}
	
	private final static Pattern varPattern = Pattern.compile("^[a-zA-Z0-9_\\.]+$");
	
	@Conf.Each("var")
	public void var(Config config) {
		checkConfig(config.hasKey() && config.hasValue() && config.hasDocument(), "must have key/value/document");
		String varname = config.valueAsString();
		checkConfig(varPattern.matcher(varname).matches(), "var name must only be letters, numbers, or underscore (and can include dots for nested properties)");
		Iterable<String> it = Splitter.on(".").split(varname);
		List<String> varparts = asList(Iterables.toArray(it, String.class));
		String escaped = config.documentContentAsString().replace("\"", "\\\"").replace("\n", "\\n");
		
		if (varparts.size() == 1) {
			// simple case, no nested objects
			scripts.add(0, format("var %s = \"%s\";", varname, escaped));
		} else {
			StringBuilder script = new StringBuilder();
			for (int i = 0; i < varparts.size(); i++) {
				String path = Joiner.on(".").join(varparts.subList(0, i + 1));
				if (i == 0) {
					// first part
					script.append(format("var %s = typeof %s === 'undefined' ? {} : %s;\n", path, path, path));		
				} else if (i < varparts.size() - 2) {
					// intermediate objects
					script.append(format("%s = {};\n", path));
				}  else {
					// the actual value
					script.append(format("%s = \"%s\";\n", path, escaped));
				}
			}
			scripts.add(0, script.toString());
		}
		
	}
	
	@Conf.EachChildOf("data-operations")
	public void op(Config config) {
		checkConfig(config.hasKey(), "must have key");
		checkConfig(config.hasDocument(), "must have document");
		ops.put(config.key(), config.documentContentAsString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setup(ModuleSetup module) {
		
		module.operation(root(), provider -> new NashornRunConfigurer(root()));
		
		module.postInit(store -> {
			
		});
		
		module.setupInitializer(init -> {
		
			init.run("initialize runtime", store -> {
				store.put(RUNNER, new PooledNashornRunner(scripts));
			});

			for (Entry<String, String> op : ops.entrySet()) {
				
				String opname = op.getKey();
				String src = op.getValue();
				
				IdentityKey<Data> dataKey = IdentityKey.named(opname);
				
				// run the js to calculate the data we need
				
				init.run(format("calculate data for %s", opname), store -> {
	
					NashornRunner js = store.get(RUNNER);
					
					Map<String,Object> m = new HashMap<>();
					m.put("data", Data.NONE);
					m.put("out", new HashMap<>());
					
					MutableData data = MutableMemoryData.create();
					Object outval = js.run(js.compile(src), m).get("out");
					if (outval instanceof Map) {
						MutableMemoryData.createFromMap((Map<String,Object>) outval).forEachContent((path, content) -> {
							data.put(path, content);
						});
					} else if (outval instanceof String) {
						data.putString(root(), (String) outval);
					} else {
						throw runtime("not sure what to do with %s", outval);
					}
					
					store.put(dataKey, data);
					
				});
				
				// the operation that will insert this data
				module.operation(path(opname), provider -> new PutJSDataConfigurer(dataKey));
			}
		
		});
		
	}
	
	public static class PutJSDataConfigurer implements OperationConfigurer {
		
		private final IdentityKey<Data> key;
		
		private Path out = root();
		
		public PutJSDataConfigurer(IdentityKey<Data> key) {
			this.key = key;
		}
		
		@Conf.Val
		public void out(String val) {
			out = dots(val);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("jsdata", store -> new PutDataOperation(store.get(key), out));
		}
		
	}

}
