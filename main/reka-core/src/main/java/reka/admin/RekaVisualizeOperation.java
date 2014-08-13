package reka.admin;

import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
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
import reka.util.Graphviz;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public class RekaVisualizeOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final Path in;
	private final Function<Data,String> format;
	private final Path out;
	
	private final Cache<HashCode,Entry<Content,Content>> cache;
	
	RekaVisualizeOperation(ApplicationManager manager, Path in, Function<Data,String> format, Path out) {
		this.manager = manager;
		this.in = in;
		this.format = format;
		this.out = out;
		
		cache = CacheBuilder.newBuilder()
					.maximumSize(100)
				.build();
	}
	
	@Override
	public MutableData call(MutableData data) {
		
		HashCode hash = data.hash(Hashing.sha1().newHasher()).hash();
		
		try {
			Entry<Content,Content> entry = cache.get(hash, () -> {
				
				Collection<FlowVisualizer> vs = manager.visualize(RekaModule.getConfigFromData(data, in));
				
				Iterator<FlowVisualizer> it = vs.iterator();
				FlowVisualizer first = null;
				
				while (it.hasNext()) {
					first = it.next();
				}
				
				if (first == null) throw runtime("no visualizers!");
				
				String formatStr = format.apply(data);
				
				String dotcontent = first.build(new DotGraphVisualizer());
				
				if ("dot".equals(formatStr)) {
					log.debug("is dot!");
					return createEntry(utf8("text/dot+plain"), 
							           utf8(dotcontent));
				}
				
				java.nio.file.Path tmp = null;
				try {
					tmp = Files.createTempFile("flow", ".dot");
			
					Graphviz.writeDotTo(dotcontent, tmp.toFile().getAbsolutePath(), formatStr);
					
					byte[] img = Files.readAllBytes(tmp);
					
					switch (formatStr) {
					case "svg":
						return createEntry(utf8("image/svg+xml"), utf8(new String(img, Charsets.UTF_8)));
					default:
						String type = String.format("image/%s", formatStr);
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