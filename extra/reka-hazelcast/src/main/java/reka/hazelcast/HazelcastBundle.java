package reka.hazelcast;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.content.Contents.binary;
import static reka.core.builder.FlowSegments.sync;
import static reka.core.config.ConfigUtils.configToData;

import java.util.function.Supplier;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.RekaBundle;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class HazelcastBundle implements RekaBundle {
	
	// TODO: what?? this is totally wrong, put needs to put to hazelcast, what does it mean?

	public void setup(Setup setup) {
		setup.use(path("hazelcast"), () -> new UseHazelcast());
	}
	
	public static class UseHazelcast extends UseConfigurer {

		@Override
		public void setup(UseInit init) {
			init.operation("put", () -> new PutConfigurer());
		}
		
	}
	
	public static class PutConfigurer implements Supplier<FlowSegment> {

		private Path out;
		private Content content;
		private Data data;
		
		@Conf.Config
		public void config(Config config) {
			if (config.hasDocument()) {
				content = binary(config.documentType(), config.documentContent());
			} else if (config.hasBody()) {
				data = configToData(config.body());
			}
		}
		
		@Conf.Val
		@Conf.At("out")
		public void out(String val) {
			out = dots(val);
		}
		
		@Override
		public FlowSegment get() {
			if (data != null) {
				return sync("put", () -> new PutDataOperation(out, data));
			} else {
				return sync("put", () -> new PutContentOperation(out, content));	
			}
		}
		
	}
	
	public static class PutDataOperation implements SyncOperation {

		private final Path out;
		private final Data payload;
		
		public PutDataOperation(Path out, Data payload) {
			this.out = out;
			this.payload = payload;
		}
		
		@Override
		public MutableData call(MutableData data) {
			return data.put(out, payload);
		}
		
	}
	
	public static class PutContentOperation implements SyncOperation {

		private final Path out;
		private final Content payload;
		
		public PutContentOperation(Path out, Content payload) {
			this.out = out;
			this.payload = payload;
		}
		
		@Override
		public MutableData call(MutableData data) {
			return data.put(out, payload);
		}
		
	}
	
}
