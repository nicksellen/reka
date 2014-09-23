package reka.builtins;

import static reka.api.Path.dots;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.trueValue;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

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
			ops.add("coerce", store -> new Coerce(coercions));
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
			ops.add("coerce/long", store -> new CoerceLong(path));
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
			ops.add("coerce/boolean", store -> new CoerceBoolean(path));
		}

	}
	
	public static class Coerce implements Operation {
		
		private final List<Entry<Path,CoercionType>> coercions;
		
		Coerce(List<Entry<Path,CoercionType>> coercions) {
			this.coercions = ImmutableList.copyOf(coercions);
		}

		@Override
		public void call(MutableData data) {
			
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
						data.put(path, Boolean.valueOf(content.asUTF8()) ? trueValue() : falseValue());
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
		public void call(MutableData data) {
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
		public void call(MutableData data) {
			Data val = data.at(path);
			if (val.isContent()) {
				data.put(path, Boolean.valueOf(val.content().asUTF8()) ? trueValue() : falseValue());
			}
		}
		
	}
	
}
