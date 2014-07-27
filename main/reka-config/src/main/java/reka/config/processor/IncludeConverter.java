package reka.config.processor;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.FileSource;
import reka.config.NavigableConfig;
import reka.config.Source;
import reka.config.StringSource;
import reka.config.parser.ConfigParser;
import reka.config.parser.values.KeyVal;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;

public final class IncludeConverter implements ConfigConverter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final List<String> CONFIG_TYPES = asList("conf", "config", "reka");
	
	private static final Pattern KEY_INCLUDE = Pattern.compile("^@include(?:\\(([^\\s\\(\\)]*)\\))?$");
	
	private static final Pattern VAL_INCLUDE = Pattern.compile("^(?:(.+)\\s+)?@include(?:\\(([^\\s\\(\\)]*)\\))?$");
    
    @Override
    public void convert(Config config, Output out) {
    	
    	if (config.hasKey()) {
    		
    		Matcher m = KEY_INCLUDE.matcher(config.key());
    		
    		if (m.matches()) {
    			includeFromKey(config, out, m);
    			return;
    		}
    		
    	}
    	
    	if (config.hasValue()) {
    		Matcher m = VAL_INCLUDE.matcher(config.valueAsString());
    		
    		if (m.matches()) {
    			
    			String val = m.group(1);
    			String loc = m.group(2);
    			
    			if (val != null) val = val.trim();
    			
    			if (config.hasBody()) {
    				checkArgument(loc == null, 
    						"if you use @include with a body, you must not include the location like @include(<location>), "
    						+ "put it in the body as a 'from' key");
    				includeFromValueWithBody(new KeyVal(config), val, config.source(), config.body(), out);
    			} else {
    				includeFromValueWithoutBody(new KeyVal(config), val, config.source(), loc, out);
    			}
    			return;
    		}
    	}
    	
    	
    	if (config.hasValue() && config.valueAsString().contains("@include")) {
    		throw new RuntimeException(format("we failed to process the @include val [%s] in [%s]", config.valueAsString(), config));
    	}

		out.add(config);
		
    }

    private void includeFromValueWithoutBody(KeyVal key, String val, Source source, String location, Output out) {
    	String type = locationToType(location);
    	if (CONFIG_TYPES.contains(type)) {
    		includeNestedConfig(key, val, source, location, out);
    	} else {
    		includeDocument(key, val, type, source, location, out);
    	}
    	
	}
    
	private void includeFromValueWithBody(KeyVal key, String val, Source source, ConfigBody body, Output out) {
		
		Optional<Config> fromO = body.at("from");
		checkArgument(fromO.isPresent(), "must include a 'from' key");
		Config from = fromO.get();
		checkArgument(from.hasValue(), "from must have a value");
		String location = from.valueAsString();

		Optional<Config> typeO = body.at("as");
		Optional<String> contentType = typeO.isPresent() ? Optional.of(typeO.get().valueAsString()) : Optional.<String>absent();
		
		if (!contentType.isPresent()) {
			contentType = Optional.of(locationToType(location));
		}
		
		if ((contentType.isPresent() && CONFIG_TYPES.contains(contentType.get())) ||
			CONFIG_TYPES.contains(locationToType(location))) {
			includeNestedConfig(key, val, source, location, out);
		} else {
			includeDocument(key, val, contentType.orNull(), source, location, out);
		}
    	
	}
	
	private void includeDocument(KeyVal key, Object val, String contentType, Source source, String location, Output out) {

		log.debug("including doc [{}] [{}] from {}\n", location, contentType, source);
		
		if (source.supportsNestedFile()) {
			try {
				out.doc(key, val, contentType, Files.toByteArray(source.nestedFile(location)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if (source.supportsNestedData()) {
			out.doc(key, val, contentType, source.nestedData(location));
		} else {
			throw new IllegalArgumentException(format(
					"you cannot use @include's unless the config source supports nested sources (e.g. file), you were using a %s", 
    				source.getClass()));
		}
		
	}
	
	private void includeConfig(Source source, String location, Output out) {
		log.debug("including conf [{}] from {}\n", location, source);
		out.add(loadNestedConfig(source, location));
	}

	private void includeNestedConfig(KeyVal key, String val, Source source, String location, Output out) {
		log.debug("including nested conf [{}] with key/val [{}] / [{}] from {}\n", location, key, val, source);
		out.obj(key, val, loadNestedConfig(source, location));
	}


	private void includeFromKey(Config config, Output out, Matcher m) {
		checkArgument(!config.hasValue(), "must not include a value with @include");
		checkArgument(!config.hasBody(), "cannot have a body with an @include as key");
		checkArgument(!config.hasDocument(), "cannot have a document with an @include as key");
		String location = m.group(1);
		String type = locationToType(location);
		log.debug("including from key [{}] groupcount={} g=[{}] type=[{}]\n", config.key(), m.groupCount(), m.group(), type);
		checkArgument("config".equals(type), "you can only @include config from a key, not %s", type);
		includeConfig(config.source(), location, out);
	}

	private NavigableConfig loadNestedConfig(Source source, String location) {

		Source nestedSource;
		
		if (source.supportsNestedFile()) {
			File nestedFile = source.nestedFile(location);
			File constraint = source.isConstrained() ? source.constraint() : nestedFile.getParentFile();
			nestedSource = FileSource.from(nestedFile, constraint, source);
		} else if (source.supportsNestedData()) {
			nestedSource = StringSource.from(new String(source.nestedData(location), Charsets.UTF_8), source);
		} else {
			throw new IllegalArgumentException(format(
					"you cannot use @include's unless the config source supports nested sources (e.g. file), you were using a %s", 
    				source.getClass()));
		}
		
		return ConfigParser.fromSource(nestedSource);
	}
	
	private static final Pattern EXT = Pattern.compile("\\.([^\\.]+)$");
	private static String locationToType(String location) {
		Matcher m = EXT.matcher(location);
		if (m.find()) {
			String ext = m.group(1);
			if (CONFIG_TYPES.contains(ext)) {
				return "config";
			} else {
				switch (ext) {
				case "css":
				case "html":
					return format("text/%s", ext);
				case "js":
				case "javascript":
					return "application/javascript";
				default:
					return ext;
				}
			}
		} else {
			return "unknown";
		}
	}
    
}