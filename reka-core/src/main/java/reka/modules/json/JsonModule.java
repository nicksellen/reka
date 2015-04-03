package reka.modules.json;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.codehaus.jackson.map.ObjectMapper;

import reka.api.Path;
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
import reka.util.StringWithVars;

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
		
		private Function<Data,Path> inFn, outFn;
		
		@Conf.Val
		@Conf.At("in")
		@Conf.At("from")
		public void in(String val) {
			inFn = StringWithVars.compile(val).andThen(s -> dots(s));
			if (outFn == null) outFn = inFn;
		}

		@Conf.At("out")
		@Conf.At("into")
		public void out(String val) {
			outFn = StringWithVars.compile(val).andThen(s -> dots(s));
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(inFn, "in is required");
			errors.checkConfigPresent(outFn, "out is required");
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("parse", () -> new JsonParseOperation(inFn, outFn));
		}
		
	}
	
	public static class JsonStringifyConfigurer implements OperationConfigurer, ErrorReporter {
		
		private Function<Data,Path> inFn, outFn;
		
		@Conf.Val
		@Conf.At("in")
		@Conf.At("from")
		public void in(String val) {
			inFn = StringWithVars.compile(val).andThen(s -> dots(s));
			if (outFn == null) outFn = inFn;
		}

		@Conf.At("out")
		@Conf.At("into")
		public void out(String val) {
			outFn = StringWithVars.compile(val).andThen(s -> dots(s));
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(inFn, "in is required");
			errors.checkConfigPresent(outFn, "out is required");
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("stringify", () -> new JsonStringifyOperation(inFn, outFn));
		}
		
	}
	
	public static class JsonParseOperation implements Operation {
		
		private final Function<Data,Path> inFn, outFn;
		
		public JsonParseOperation(Function<Data,Path> inFn, Function<Data,Path> outFn) {
			this.inFn = inFn;
			this.outFn = outFn;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			Data val = data.at(inFn.apply(data));
			if (val.isContent()) {
				try {
					@SuppressWarnings("unchecked")
					Map<String,Object> map = jsonMapper.readValue(val.content().asUTF8(), Map.class);
					data.put(outFn.apply(data), MutableMemoryData.createFromMap(map));
				} catch (IOException e) {
					throw unchecked(e);
				}
			}
		}
		
	}
	
	public static class JsonStringifyOperation implements Operation {
		
		private final Function<Data,Path> inFn, outFn;
		
		public JsonStringifyOperation(Function<Data,Path> inFn, Function<Data,Path> outFn) {
			this.inFn = inFn;
			this.outFn = outFn;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.putString(outFn.apply(data), data.at(inFn.apply(data)).toJson());
		}
		
	}
	
}
