package reka.less;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.slashes;
import static reka.api.content.Contents.utf8;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.lesscss.Resource;

import reka.api.Path;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class LessModule extends ModuleConfigurer {
	
	private final LessCompiler compiler;
	
	public LessModule(LessCompiler compiler) {
		this.compiler = compiler;
	}

	@Override
	public void setup(ModuleSetup module) {
		module.operation(root(), provider -> new LessConfigurer(compiler));
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
	
	public static class LessConfigurer implements OperationConfigurer {
		
		private final LessCompiler compiler;
		
		private Function<Data,Path> outFn = (data) -> Response.CONTENT;
		private Content content;
		
		public LessConfigurer(LessCompiler compiler) {
			this.compiler = compiler;
		}
		
		@Conf.Config
		public void config(Config config) {
			try {
				if (config.hasDocument()) {
					content = utf8(compiler.compile(config.documentContentAsString()));
				} else if (config.hasBody()) {
					Map<Path,String> resources = new HashMap<>();
					for (Config child : config.body()) {
						String key = child.key();
						if (!key.endsWith(".less") && !key.endsWith(".css")) key = key + ".less";
						resources.put(slashes(key), child.documentContentAsString());
					}
					checkConfig(resources.containsKey(path("main.less")), "must include a main");
					java.nio.file.Path dir = Files.createTempDirectory("tmp.less");
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
						content = utf8(compiler.compile(dir.resolve("main.less").toFile(), "main.less"));
					} finally {
						File f = dir.toFile();
						if (f.exists()) {
							f.delete();
						}
					}
				}
			} catch (LessException | IOException e) {
				throw unchecked(e);
			}
		}

		public void setup(OperationSetup ops) {
			ops.add("compile", store -> new LessCompileOperation(outFn, content));
		}
		
	}
	
	public static class LessCompileOperation implements Operation {
		
		private final Function<Data,Path> outFn;
		private final Content content;
		
		public LessCompileOperation(Function<Data,Path> outFn, Content content) {
			this.outFn = outFn;
			this.content = content;
		}

		@Override
		public void call(MutableData data) {
			data.put(outFn.apply(data), content);
		}
		
	}

}
