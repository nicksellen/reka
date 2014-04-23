package reka.jade;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Hashable;
import reka.api.JsonProvider;
import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.util.ReadObservedMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;

import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;

public class JadeWithSomeCachingStuff implements SyncOperation, JsonProvider {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("jade");

	private static final BaseEncoding HEX_ENCODING = BaseEncoding.base16();

	private static final HashFunction hash =  Hashing.sha1();

	protected final JadeConfiguration jade = new JadeConfiguration();

	protected final JadeTemplate template;
	private final byte[] templateHash;
	protected final Path inputPath, outputPath;
	protected final boolean mainResponse;
	
	private final AtomicBoolean useCache;
	
	private final Cache<String,String> renderedTemplateCache = CacheBuilder.newBuilder()
			.maximumSize(1000)
		.build();
	
	private final ConcurrentMap<Path,Object> valuePaths = new ConcurrentHashMap<>();
	private final ConcurrentMap<Path,Object> iteratedPaths = new ConcurrentHashMap<>();

	public JadeWithSomeCachingStuff(String value, Path inputPath, Path outputPath, boolean attemptCache) {
		checkArgument(value != null, "must pass a filepath or template text");
		
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		useCache = new AtomicBoolean(attemptCache);
		mainResponse = outputPath.equals(Response.CONTENT);
		
		templateHash = hash.newHasher()
			.putString(value)
			.putBytes(inputPath.toByteArray())
			.putBytes(outputPath.toByteArray()).hash().asBytes();

		try {
			final String text = new File(value).isFile() ? Files.toString(
					new File(value), Charset.defaultCharset()) : value;

			jade.setTemplateLoader(new TemplateLoader() {

				public Reader getReader(String name) throws IOException {
					// we're not using their stupid loader
					// we ALWAYS return the same text regardless of the name
					// used
					// as this loader only ever gets used for this template
					return new StringReader(text);
				}

				public long getLastModified(String name) throws IOException {
					return 0;
				}

			});
			
			template = jade.getTemplate(null);

		} catch (Exception e) {
			throw new RuntimeException(format("could not create jade template"), e);
		}
	}

	private ReadObservedMap<String, Object> observedMap(Data data) {
		return ReadObservedMap.wrap(data.at(inputPath).toMap());
	}
	
	protected Map<String, Object> simpleMap(Data data) {
		return data.at(inputPath).toMap();
	}
	
	@Override
	public MutableData call(MutableData data) {
		return useCache.get() ? runWithCache(data) : 
			data.putString(outputPath, jade.renderTemplate(template, simpleMap(data)));
	}
		
	private MutableData runWithCache(MutableData data) {

		String etag = calculateETagFor(data);
		
		if (etag == null) {
			useCache.set(false);
		}
		
		//data.getString(Request.Headers.IF_NONE_MATCH).
		if (etag != null && mainResponse && etag.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
			return data.putInt(Response.STATUS, 304).putString(Response.CONTENT, "");
		}
		
		String renderedTemplate = null;
		
		if (etag != null) {
			renderedTemplate = renderedTemplateCache.getIfPresent(etag);
		}
		
		if (renderedTemplate == null) {
			ReadObservedMap<String,Object> map = observedMap(data);
			renderedTemplate = jade.renderTemplate(template, map);
			
			if (etag != null) {
				renderedTemplateCache.put(etag, renderedTemplate);				
			}
			
			boolean changed = false;
			
			for (Path path : map.valuePaths()) {
				Object previous = valuePaths.putIfAbsent(inputPath.add(path), 0);
				if (previous == null) {
					changed = true;
				}
			}
			
			for (Path path : map.iteratedPaths()) {
				Object previous = iteratedPaths.putIfAbsent(inputPath.add(path), 0);
				if (previous == null) {
					changed = true;
				}
			}

			if (changed) {
				etag = calculateETagFor(data);
			}
		}
		
		if (etag != null) {
			
			if (mainResponse && etag.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
				return data.putInt(Response.STATUS, 304).putString(Response.CONTENT, "");
			}

			if (mainResponse) {
				data.putString(Response.CONTENT, renderedTemplate)
					.putString(Response.Headers.CONTENT_TYPE, "text/html")
					.putString(Response.Headers.ETAG, etag)
					.putString(Response.Headers.CACHE_CONTROL, "no-cache"); // use etag only
			} else {
				data.putString(outputPath, renderedTemplate);
			}
			
		} else {
			
			if (mainResponse) {
				data.putString(Response.CONTENT, renderedTemplate)
					.putString(Response.Headers.CONTENT_TYPE, "text/html");
			} else {
				data.putString(outputPath, renderedTemplate);	
			}
			
		}
		
		return data;
	}
	
	private String calculateETagFor(MutableData data) {
		
		Hasher hasher = hash.newHasher();
		
		hasher.putInt(data.at(inputPath).size());
		
		Set<Path> valuePathKeys = valuePaths.keySet();
		hasher.putInt(valuePathKeys.size());
		for (Path path : valuePathKeys) {
			Object value = data.getContent(path).orElse(null);
			hasher.putBytes(path.toByteArray());
			
			if (!hash(value, hasher)) {
				return null;
			}
			
			hasher.putByte((byte) 0);
		}
		
		hasher.putByte((byte) 1);
		
		Set<Path> iteratedPathKeys = iteratedPaths.keySet();
		hasher.putInt(iteratedPathKeys.size());
		for (Path path : iteratedPathKeys) {
			for (PathElement e : data.at(path).elements()) {
				e.hash(hasher);
			}
			hasher.putByte((byte) 0);
		}

		hasher.putByte((byte) 1);
		
		hasher.putBytes(templateHash);
		
		return HEX_ENCODING.encode(hasher.hash().asBytes());
	}
	
	private boolean hash(Object value, Hasher hasher) {
		if (value instanceof Hashable) {
			((Hashable) value).hash(hasher);
		} else if (value instanceof String) {
			hasher.putString((String) value);
		} else if (value instanceof Long) {
			hasher.putLong((long) value);
		} else if (value instanceof Integer) {
			hasher.putInt((int) value);
		} else if (value instanceof Double) {
			hasher.putDouble((double) value);
		} else if (value instanceof Float) {
			hasher.putFloat((float) value);
		} else if (value instanceof Boolean) {
			hasher.putBoolean((Boolean) value);
		} else if (value == null) {
			hasher.putByte((byte) 0);
		} else if (value instanceof Map) {
			Map<?,?> map = (Map<?,?>) value;
			hasher.putInt(map.size());
			for (Entry<?, ?> entry : map.entrySet()) {
				if (!hash(entry.getKey(), hasher) || 
					!hash(entry.getValue(), hasher)) {
					return false;
				}
			}
		} else if (value instanceof List) {
			List<?> list = (List<?>) value;
			hasher.putInt(list.size());
			for (Object listValue : list) {
				if (!hash(listValue, hasher)) {
					return false;
				}
			}
		} else {
			log.debug("not calculating hash because there was a {} which we couldn't hash", value != null ? value.getClass() : null);
			return false;
		}
		
		return true;
	}

	@Override
	public void out(JsonGenerator json) throws IOException {

		json.writeStartObject();
		
		if (useCache.get()) {
			CacheStats stats = renderedTemplateCache.stats();
			
			json.writeFieldName("cache");
			json.writeStartObject();
				json.writeStringField("hit-rate", format("%.2f", stats.hitRate()));
				json.writeNumberField("request-count", stats.requestCount());
				json.writeNumberField("entries", renderedTemplateCache.size());
			json.writeEndObject();

			json.writeFieldName("hash-paths");
			json.writeStartObject();
			
				json.writeFieldName("value");
				json.writeStartArray();
					for (Path entry : valuePaths.keySet()) {
						json.writeString(entry.dots());
					}
				json.writeEndArray();
	
				json.writeFieldName("iterated");
				json.writeStartArray();
					for (Path entry : iteratedPaths.keySet()) {
						json.writeString(entry.dots());
					}
				json.writeEndArray();
			json.writeEndObject();
			
		}
		json.writeEndObject();
	}
	
	public static class Uncached extends JadeWithSomeCachingStuff {

		public Uncached(String value, Path inputPath, Path outputPath) {
			super(value, inputPath, outputPath, false);
		}

		@Override
		public MutableData call(MutableData data) {
			if (mainResponse) data.putString(Response.Headers.CONTENT_TYPE, "text/html");
			return data.putString(outputPath, jade.renderTemplate(template, simpleMap(data)));
		}
		
	}
}
