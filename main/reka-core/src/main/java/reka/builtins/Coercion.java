package reka.builtins;

import static reka.api.Path.dots;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.trueValue;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.configurer.annotations.Conf;

import com.google.common.collect.ImmutableList;

public class Coercion {
	
	public static enum CoercionType {
		LONG, BOOLEAN 
	}

	public static class CoerceConfigurer implements Supplier<FlowSegment> {

		private final List<Entry<Path,CoercionType>> coercions = new ArrayList<>();
		
		@Conf.Each
		public void config(Config config) {
			checkConfig(config.hasValue(), "must have value");
			coercions.add(createEntry(dots(config.valueAsString()), CoercionType.valueOf(config.key().toUpperCase())));
			
		}
		
		@Override
		public FlowSegment get() {
			return sync("coerce", () -> new Coerce(coercions));
		}

	}
	
	public static class CoerceLongConfigurer implements Supplier<FlowSegment> {

		private Path path;
		
		@Conf.Val
		public void path(String val) {
			path = dots(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("coerce/long", () -> new CoerceLong(path));
		}

	}
	
	public static class CoerceBooleanConfigurer implements Supplier<FlowSegment> {

		private Path path;
		
		@Conf.Val
		public void path(String val) {
			path = dots(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("coerce/boolean", () -> new CoerceBoolean(path));
		}

	}
	
	public static class Coerce implements SyncOperation {
		
		private final List<Entry<Path,CoercionType>> coercions;
		
		Coerce(List<Entry<Path,CoercionType>> coercions) {
			this.coercions = ImmutableList.copyOf(coercions);
		}

		@Override
		public MutableData call(MutableData data) {
			
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
			return data;
		}
		
	}
	
	public static class CoerceLong implements SyncOperation {
		
		private final Path path;
		
		CoerceLong(Path path) {
			this.path = path;
		}

		@Override
		public MutableData call(MutableData data) {
			Data val = data.at(path);
			if (val.isContent()) {
				Content content = val.content();
				data.put(path, longValue(Long.valueOf(content.asUTF8())));
			}
			return data;
		}
		
	}
	
	public static class CoerceBoolean implements SyncOperation {
		
		private final Path path;
		
		CoerceBoolean(Path path) {
			this.path = path;
		}

		@Override
		public MutableData call(MutableData data) {
			Data val = data.at(path);
			if (val.isContent()) {
				Content content = val.content();
				data.put(path, Boolean.valueOf(content.asUTF8()) ? trueValue() : falseValue());
			}
			return data;
		}
		
	}
	
}