package reka.admin;

import static reka.api.Path.slashes;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.builder.DotGraphVisualizer;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.JsonGraphVisualizer;
import reka.util.Graphviz;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class VisualizeAppOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final Function<Data,String> formatFn;
	private final Function<Data,String> appIdentityFn;
	private final Function<Data,String> flowNameFn;
	private final Path out;
	
	private final Cache<HashCode,Entry<Content,Content>> cache;
	
	VisualizeAppOperation(ApplicationManager manager, 
						  Function<Data,String> appIdentityFn,
						  Function<Data,String> flowNameFn, 
						  Function<Data,String> format,
						  Path out) {
		this.manager = manager;
		this.appIdentityFn = appIdentityFn;
		this.flowNameFn = flowNameFn;
		this.formatFn = format;
		this.out = out;
		
		cache = CacheBuilder.newBuilder()
					.maximumSize(100)
				.build();
	}
	
	@Override
	public MutableData call(MutableData data) {
		
		String identity = appIdentityFn.apply(data);
		Path flowName = slashes(flowNameFn.apply(data));
		String format = formatFn.apply(data);
		
		Hasher hasher = Hashing.sha1().newHasher()
			.putString(identity)
			.putInt(manager.version(identity));

		flowName.hash(hasher);
		
		hasher.putString(format);
		
		HashCode hash = hasher.hash();
		
		log.info("making visualization of {}:{} in {}", identity, flowName.slashes(), format);

		try {
			Entry<Content,Content> entry = cache.get(hash, () -> {
				
				FlowVisualizer vis = manager.visualize(identity, flowName).orElseThrow(() -> 
					runtime("no visualization available for %s:%s :(", identity, flowName.slashes()));
				
				if ("json".equals(format)) {
					log.debug("is json!");
					return createEntry(utf8("application/json"), 
							           utf8(vis.build(new JsonGraphVisualizer())));
				}
				
				String dotcontent = vis.build(new DotGraphVisualizer());
				
				if ("dot".equals(format)) {
					log.debug("is dot!");
					return createEntry(utf8("text/dot+plain"), 
							           utf8(dotcontent));
				}
				
				java.nio.file.Path tmp = null;
				try {
					tmp = Files.createTempFile("flow", ".dot");
			
					Graphviz.writeDotTo(dotcontent, tmp.toFile().getAbsolutePath(), format);
					
					byte[] img = Files.readAllBytes(tmp);
					
					switch (format) {
					case "svg":
						return createEntry(utf8("image/svg+xml"), utf8(new String(img, Charsets.UTF_8)));
					default:
						String type = String.format("image/%s", format);
						return createEntry(utf8(type), binary(type, img));
					}
					

				} catch (IOException e) {
					throw unchecked(e);
					
				} finally {
					if (tmp != null) {
						tmp.toFile().delete();
					}
				}
			});
			
			if (out.equals(Response.CONTENT)) {
				
				data.put(Response.Headers.CONTENT_TYPE, entry.getKey())
					.put(Response.CONTENT, entry.getValue());
			
			} else {
				
				data.put(out, entry.getValue());
				
			}
			
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
}