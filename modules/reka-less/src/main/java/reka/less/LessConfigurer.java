package reka.less;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.slashes;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.deleteRecursively;
import static reka.util.Util.sha1hex;
import static reka.util.Util.unchecked;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.lesscss.Resource;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.content.Contents;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.dirs.AppDirs;

public class LessConfigurer extends ModuleConfigurer {
	
	private final LessCompiler compiler;
	
	public LessConfigurer(LessCompiler compiler) {
		this.compiler = compiler;
	}

	@Override
	public void setup(ModuleSetup module) {
		module.operation(root(), provider -> new LessContentConfigurer(compiler, dirs()));
	}
	
	public static class ConfigResource implements Resource {
		
		private final Map<Path,String> resources;
		private final Path path;
		
		public ConfigResource(Path path, Map<Path,String> resources) {
			this.path = path;
			this.resources = resources;
		}

		@Override
		public boolean exists() {
			return resources.containsKey(path);
		}

		@Override
		public long lastModified() {
			return 0;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(resources.get(path).getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public Resource createRelative(String relativeResourcePath) throws IOException {
			return new ConfigResource(path.add(slashes(relativeResourcePath)), resources);
		}

		@Override
		public String getName() {
			return path.slashes();
		}
		
		@Override
		public String toString() {
			return format("%s(%s)", getClass().getSimpleName(), getName());
		}
		
	}
	
	public static class LessContentConfigurer implements OperationConfigurer {
		
		private final LessCompiler compiler;
		private final AppDirs dirs;
		
		private Function<Data,Path> outFn = (data) -> Response.CONTENT;
		private Content content;
		
		public LessContentConfigurer(LessCompiler compiler, AppDirs dirs) {
			this.compiler = compiler;
			this.dirs = dirs;
		}
		
		@Conf.Config
		public void config(Config config) {
			if (config.hasDocument()) {
				try {
					content = utf8(compiler.compile(config.documentContentAsString()));
				} catch (LessException e) {
					throw unchecked(e);
				}
			} else if (config.hasBody()) {
				Map<Path,String> resources = new HashMap<>();
				for (Config child : config.body()) {
					String key = child.key();
					if (!key.endsWith(".less") && !key.endsWith(".css")) key = key + ".less";
					resources.put(slashes(key), child.documentContentAsString());
				}
				content = toCssContent(dirs.tmp(), compileLess(compiler, resources));
			}
		}

		public void setup(OperationSetup ops) {
			ops.add("css", store -> new LessOperation(outFn, content, sha1hex(content.asBytes())));
		}
		
	}
	
	private static final Content TEXT_CSS = utf8("text/css");
	
	private static final Content EMPTY = Contents.nullValue();	
	private static final Content NOT_MODIFIED = integer(304);
	
	public static class LessOperation implements Operation {
		
		private final Function<Data,Path> outFn;
		private final Content content;
		
		private final String etagValue;
		private final Content etag;
		
		public LessOperation(Function<Data,Path> outFn, Content content, String etagValue) {
			this.outFn = outFn;
			this.content = content;
			this.etagValue = etagValue;
			etag = utf8(etagValue);
		}

		@Override
		public void call(MutableData data) {

			Path out = outFn.apply(data);
			
			if (out.equals(Response.CONTENT)) {
				if (data.existsAt(Request.Headers.IF_NONE_MATCH) && etagValue.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
					data.put(Response.CONTENT, EMPTY)
						.put(Response.STATUS, NOT_MODIFIED);
				} else {
					data.put(Response.CONTENT, content)
						.put(Response.Headers.CONTENT_TYPE, TEXT_CSS)
						.put(Response.Headers.ETAG, etag);					
				}
			} else {
				data.put(out, content);
			}
		}
		
	}
	
	private static String compileLess(LessCompiler compiler, Map<Path,String> resources) {
		checkConfig(resources.containsKey(path("main.less")), "must include a main");
		try {
			java.nio.file.Path dir = Files.createTempDirectory("reka.less.");
			try {
				resources.forEach((path, content) -> {
					try {
						java.nio.file.Path resolved = dir.resolve(path.slashes()).normalize();
						checkConfig(resolved.startsWith(dir), "invalid path %s", path.slashes());
						Files.write(resolved, content.getBytes(StandardCharsets.UTF_8));
					} catch (Exception e) {
						throw unchecked(e);
					}
				});
				return compiler.compile(dir.resolve("main.less").toFile(), "main.less");
			} catch (LessException e) {
				throw unchecked(e);
			} finally {
				deleteRecursively(dir);
			}
		} catch (IOException e1) {
			throw unchecked(e1);
		}
	}
	
	private static final long PUT_IN_FILE_THRESHOLD = 1024L * 32l; // 32k;
	private static final String CONTENT_TYPE_CSS = "text/css";
	
	private static Content toCssContent(java.nio.file.Path tmpdir, String css) {
		byte[] contentBytes = css.getBytes(StandardCharsets.UTF_8);
		if (contentBytes.length > PUT_IN_FILE_THRESHOLD) {
			try {
				String hex = sha1hex(contentBytes);
				java.nio.file.Path httpfile = tmpdir.resolve("less." + hex + ".css");
				if (!Files.exists(httpfile)) Files.write(httpfile, contentBytes);
				return binary(CONTENT_TYPE_CSS, httpfile.toFile());
			} catch (IOException e) {
				throw unchecked(e);
			}
		} else {
			ByteBuffer buf = ByteBuffer.allocateDirect(contentBytes.length).put(contentBytes);
			buf.flip();
			return binary(CONTENT_TYPE_CSS, buf.asReadOnlyBuffer());
		}
	}

}
