package reka.modules.admin;

import static reka.api.Path.slashes;
import static reka.data.content.Contents.binary;
import static reka.data.content.Contents.utf8;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.Response;
import reka.app.manager.ApplicationManager;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.flow.builder.DotGraphVisualizer;
import reka.flow.builder.FlowVisualizer;
import reka.flow.builder.JsonGraphVisualizer;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Graphviz;
import reka.util.Identity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class VisualizeAppOperation implements Operation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final Function<Data,String> formatFn;
	private final Function<Data,Path> appIdentityFn;
	private final Function<Data,String> flowNameFn;
	private final Path out;
	private final String stylesheet;
	
	private final Cache<HashCode,Entry<Content,Content>> cache;
	
	VisualizeAppOperation(ApplicationManager manager, 
						  Function<Data,Path> appIdentityFn,
						  Function<Data,String> flowNameFn, 
						  Function<Data,String> format,
						  Path out,
						  String stylesheet) {
		this.manager = manager;
		this.appIdentityFn = appIdentityFn;
		this.flowNameFn = flowNameFn;
		this.formatFn = format;
		this.out = out;
		this.stylesheet = stylesheet;
		
		cache = CacheBuilder.newBuilder().maximumSize(200).build();
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {

		Path appPath = appIdentityFn.apply(data);
		Identity identity = manager.identityFor(appPath);
		
		Path flowName = slashes(flowNameFn.apply(data));
		String format = formatFn.apply(data);
		
		Hasher hasher = Hashing.sha1().newHasher()
			.putString(identity.name(), StandardCharsets.UTF_8)
			.putInt(manager.version(identity));

		flowName.hash(hasher);
		
		hasher.putString(format, StandardCharsets.UTF_8);
		
		HashCode hash = hasher.hash();
		
		log.debug("making visualization of {}:{} in {}", appPath.slashes(), flowName.slashes(), format);

		try {
			Entry<Content,Content> entry = cache.get(hash, () -> {
				
				FlowVisualizer vis = manager.visualize(identity, flowName).orElseThrow(() -> 
					runtime("no visualization available for %s:%s :(", appPath.slashes(), flowName.slashes()));
				
				if ("json".equals(format)) {
					return createEntry(utf8("application/json"), 
							           utf8(vis.build(new JsonGraphVisualizer())));
				}
				
				String dotcontent = vis.build(new DotGraphVisualizer());
				
				if ("dot".equals(format)) {
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
						String svg = new String(img, StandardCharsets.UTF_8);
						if (stylesheet != null) {
							int i = svg.indexOf("<svg ");
							if (i > -1) {
								StringBuilder sb = new StringBuilder(svg);
								sb.insert(i, String.format("<?xml-stylesheet type=\"text/css\" href=\"%s\" ?>\n", stylesheet));
								svg = sb.toString();
							}
						}
						return createEntry(utf8("image/svg+xml"), utf8(svg));
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
	}
	
}