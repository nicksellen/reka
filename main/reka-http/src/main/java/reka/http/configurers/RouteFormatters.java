package reka.http.configurers;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static reka.util.Util.unchecked;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.Hashable;
import reka.http.operations.HttpRouter.RouteFormatter;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hasher;

public abstract class RouteFormatters implements Hashable {
	
	private static final Pattern TEMPLATE = Pattern.compile("(?:\\{([a-zA-Z0-9_-]+\\*?\\??)\\})");
	
	public static RouteFormatter create(String template) {
		Matcher matcher = TEMPLATE.matcher(template);
		if (matcher.find()) {
			return new RouterFormatterWithKeys(template, matcher.reset());
		} else {
			return new StaticRouterFormatter(template);
		}
	}
	
	static class StaticRouterFormatter implements RouteFormatter {
		
		private final String value;
		
		public StaticRouterFormatter(String value) {
			this.value = value;
		}

		@Override
		public String url(Map<String, Object> parameters) {
			return value;
		}
		
		@Override
		public String url(Object... parameters) {
			return value;
		}
		
		@Override
		public String url() {
			return value;
		}
		
		@Override
		public String toString() {
			return String.format("<%s template=%s>", getClass().getSimpleName(), value);
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putString(value);
		}
		
		
	}
	
	static class RouterFormatterWithKeys implements RouteFormatter {
		
		private final static Map<String,Object> EMPTY_PARAMS = ImmutableMap.of();
		
		private final List<Key> keys;
		private final String format;
		private final boolean allOptional;
		
		private static class Key implements Hashable {
			String name;
			boolean starred = false;
			boolean optional = false;
			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof Key)) {
					return false;
				}
				Key other = (Key) obj;
				return other.starred == starred && 
					   other.optional == optional &&
					   other.name.equals(name);
			}
			@Override
			public int hashCode() {
				return Objects.hash(name, starred, optional);
			}
			@Override
			public String toString() {
				return String.format("<Key name=%s starred=%s required=%s>", name, starred, !optional);
			}
			@Override
			public Hasher hash(Hasher hasher) {
				return hasher.putString(name).putBoolean(starred).putBoolean(optional);
			}
		}
		
		public RouterFormatterWithKeys(String template, Matcher matcher) {
			
			// TODO: this should be a bit better really, I need to know which parameter is
			// a) required and/or b) starred (to decide if to escape / -> %2F)
			
			StringBuilder format = new StringBuilder();
			
			int pos = 0;
			Key key;
			
			boolean _allOptional = true;
			
			List<Key> _keys = new ArrayList<>();
			
			while (matcher.find()) {
				
				if (pos != matcher.start()) {
					format.append(template.substring(pos, matcher.start()));
				}
				
				key = new Key();
				key.name = matcher.group(1);
				
				if (key.name.endsWith("?")) {
					key.optional = true;
					key.name = key.name.substring(0, key.name.length() - 1);
				}
				
				if (key.name.endsWith("*")) {
					key.starred = true;
					key.name = key.name.substring(0, key.name.length() - 1);
				}
				
				if (_keys.contains(key)) {
					throw new RuntimeException(
							String.format("%s cannot have multiple keys with the same name", getClass().getSimpleName()));
				}
				
				_keys.add(key);
				
				if (!key.optional) {
					_allOptional = false;
				}
				
				format.append("%s");
				
				pos = matcher.end();
			}
			
			if (pos < template.length()) {
				format.append(template.substring(pos));
			}

			this.keys = ImmutableList.copyOf(_keys);
			this.allOptional = _allOptional;
			this.format = format.toString();
		}
		
		@Override
		public String url(Map<String,Object> parameters) {
			Object[] values = new String[keys.size()];
			for (int i = 0; i < keys.size(); i++) {
				Key key = keys.get(i);
				Object value = parameters.get(key.name);
				values[i] = processKeyAndValue(key, value);
			}
			return String.format(format, values);
		}
		
		@Override
		public String url(Object... parameters) {
			checkArgument(parameters.length == keys.size(), 
					"you passed in %d things, we wanted %d (for route formatter %s)", parameters.length, keys.size(), format);
			
			Object[] values = new String[keys.size()];
			for (int i = 0; i < keys.size(); i++) {
				Key key = keys.get(i);
				Object value = parameters[i];
				values[i] = processKeyAndValue(key, value);
			}
			return String.format(format, values);
		}
		
		private Object processKeyAndValue(Key key, Object value) {
			if (value == null) {
				if (key.optional) {
					return "";	
				} else {
					throw new RuntimeException(String.format("key %s is required", key.name));
				}
			} else {
				if (key.starred) {
					return value;
				} else {
					try {
						return URLEncoder.encode(value.toString(), Charsets.UTF_8.name());
					} catch (UnsupportedEncodingException e) {
						throw unchecked(e);
					}
				}
			}
		}

		@Override
		public String url() {
			if (!allOptional) {
				throw new RuntimeException(
						format("one or more keys are NOT optional %s", keys));
			}
			return url(EMPTY_PARAMS);
		}
		
		@Override
		public String toString() {
			return String.format("<%s format=%s keys=%s>", getClass().getSimpleName(), format, keys);
		}

		public Hasher hash(Hasher hasher) {
			hasher.putString(format);
			hasher.putInt(keys.size());
			for (Key key : keys) {
				key.hash(hasher);
			}
			return hasher;
		}
	}
}