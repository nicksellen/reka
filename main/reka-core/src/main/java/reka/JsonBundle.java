package reka;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.function.Supplier;

import org.codehaus.jackson.JsonFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.RekaBundle;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;
import reka.core.data.memory.MutableMemoryData;

public class JsonBundle implements RekaBundle {

	private static final JsonFactory jsonFactory = new JsonFactory();

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("json"), () -> new UseJson());
	}
	
	public static class UseJson extends ModuleConfigurer {

		@Override
		public void setup(ModuleInit init) {
			init.operation(Path.path("parse"), () -> new JsonParseConfigurer());
		}
		
	}

	public static class JsonParseConfigurer implements Supplier<FlowSegment> {
		
		private Path in, out;
		
		@Conf.At("in")
		public void in(String val) {
			in = dots(val);
		}

		@Conf.At("out")
		public void out(String val) {
			out = dots(val);
		}

		@Override
		public FlowSegment get() {
			return sync("json/parse", () -> new JsonParseOperation(in, out));
		}
		
	}
	
	public static class JsonParseOperation implements SyncOperation {
		private final Path in, out;
		
		public JsonParseOperation(Path in, Path out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public MutableData call(MutableData data) {
			Data val = data.at(in);
			if (val.isContent()) {
				try {
					data.put(out, MutableMemoryData.readJson(jsonFactory.createJsonParser(val.content().asUTF8())));
				} catch (IOException e) {
					throw unchecked(e);
				}
			} 
			return data;
		}
		
	}
	
}
