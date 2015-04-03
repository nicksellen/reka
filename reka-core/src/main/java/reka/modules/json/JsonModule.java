package reka.modules.json;

import static reka.util.Path.dots;
import static reka.util.Path.path;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

public class JsonModule implements Module {

	@Override
	public Path base() {
		return path("json");
	}
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new JsonConfigurer());
	}
	
	public static class JsonConfigurer extends ModuleConfigurer {

		@Override
		public void setup(AppSetup module) {
			module.defineOperation(path("parse"), provider -> new JsonParseConfigurer());
			module.defineOperation(path("stringify"), provider -> new JsonStringifyConfigurer());
		}
		
	}

	public static class JsonParseConfigurer implements OperationConfigurer, ErrorReporter {
		
		private Path from, into;
		
		@Conf.Val
		@Conf.At("from")
		public void in(String val) {
			from = dots(val);
			if (into == null) into = from;
		}

		@Conf.At("into")
		public void out(String val) {
			into = dots(val);
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(from, "from is required");
			errors.checkConfigPresent(into, "into is required");
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("parse", () -> new JsonParseOperation(from, into));
		}
		
	}
	
	public static class JsonStringifyConfigurer implements OperationConfigurer, ErrorReporter {
		
		private Path from, into;
		
		@Conf.Val
		@Conf.At("from")
		public void in(String val) {
			from = dots(val);
			if (into == null) into = from;
		}

		@Conf.At("into")
		public void out(String val) {
			into = dots(val);
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(from, "from is required");
			errors.checkConfigPresent(into, "into is required");
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("stringify", () -> new JsonStringifyOperation(from, into));
		}
		
	}
	
	public static class JsonParseOperation implements Operation {
		
		private final Path from, into;
		
		public JsonParseOperation(Path from, Path into) {
			this.from = from;
			this.into = into;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			Data val = data.at(from);
			if (val.isContent()) {
				try {
					@SuppressWarnings("unchecked")
					Map<String,Object> map = jsonMapper.readValue(val.content().toString(), Map.class);
					data.put(into, MutableMemoryData.createFromMap(map));
				} catch (IOException e) {
					throw unchecked(e);
				}
			}
		}
		
	}
	
	public static class JsonStringifyOperation implements Operation {
		
		private final Path from, into;
		
		public JsonStringifyOperation(Path from, Path into) {
			this.from = from;
			this.into = into;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.putString(into, data.at(from).toJson());
		}
		
	}
	
}
