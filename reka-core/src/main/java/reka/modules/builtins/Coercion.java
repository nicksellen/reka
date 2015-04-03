package reka.modules.builtins;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.data.content.Contents.booleanValue;
import static reka.data.content.Contents.longValue;
import static reka.util.Path.dots;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

import com.google.common.collect.ImmutableList;

public class Coercion {
	
	public static enum CoercionType {
		LONG, BOOLEAN 
	}

	public static class CoerceConfigurer implements OperationConfigurer {

		private final List<Entry<Path,CoercionType>> coercions = new ArrayList<>();
		
		@Conf.Each
		public void config(Config config) {
			checkConfig(config.hasValue(), "must have value");
			coercions.add(createEntry(dots(config.valueAsString()), CoercionType.valueOf(config.key().toUpperCase())));
			
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("coerce", () -> new Coerce(coercions));
		}

	}
	
	public static class CoerceLongConfigurer implements OperationConfigurer {

		private Path path;
		
		@Conf.Val
		public void path(String val) {
			path = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("coerce/long", () -> new CoerceLong(path));
		}

	}
	
	public static class CoerceBooleanConfigurer implements OperationConfigurer {

		private Path path;
		
		@Conf.Val
		public void path(String val) {
			path = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("coerce/boolean", () -> new CoerceBoolean(path));
		}

	}
	
	public static class Coerce implements Operation {
		
		private final List<Entry<Path,CoercionType>> coercions;
		
		Coerce(List<Entry<Path,CoercionType>> coercions) {
			this.coercions = ImmutableList.copyOf(coercions);
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			
			for (Entry<Path, CoercionType> e : coercions) {
				Path path = e.getKey();
				Data val = data.at(path);
				if (val.isContent()) {
					Content content = val.content();
					switch (e.getValue()) {
					case LONG:
						data.put(path, longValue(Long.valueOf(content.asUTF8())));
						break;
					case BOOLEAN:
						data.put(path, booleanValue(Boolean.valueOf(content.asUTF8())));
						break;
					}
				}
			}
		}
		
	}
	
	public static class CoerceLong implements Operation {
		
		private final Path path;
		
		CoerceLong(Path path) {
			this.path = path;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			Data val = data.at(path);
			if (val.isContent()) {
				Content content = val.content();
				data.put(path, longValue(Long.valueOf(content.asUTF8())));
			}
		}
		
	}
	
	public static class CoerceBoolean implements Operation {
		
		private final Path path;
		
		CoerceBoolean(Path path) {
			this.path = path;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			Data val = data.at(path);
			if (val.isContent()) {
				data.put(path, booleanValue(Boolean.valueOf(val.content().asUTF8())));
			}
		}
		
	}
	
}
