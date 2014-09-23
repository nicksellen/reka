package reka.jsx;
import static reka.api.Path.root;
import static reka.api.content.Contents.utf8;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import reka.api.IdentityKey;
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


public class JsxModule extends ModuleConfigurer {
	
	protected static final IdentityKey<String> COMPILED = IdentityKey.named("compiled jsx");
	
	private final StringBuilder src = new StringBuilder();
	
	@Conf.Each("template")
	public void template(Config config) {
		checkConfig(config.hasDocument(), "template must have document");
		src.append(config.documentContentAsString()).append("\n\n");
	}
	
	@Override
	public void setup(ModuleSetup module) {
		
		module.setupInitializer(init -> {
			init.run("compile jsx", store -> {
				String jsx = src.toString();
				try {
					MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
					sha1.reset();
					sha1.update(jsx.getBytes(StandardCharsets.UTF_8));
					String hex = byteArrayToHexString(sha1.digest());
					
					String compiled;
					File cacheFile = new File("/tmp/reka.jsxcache." + hex);
					if (cacheFile.exists()) {
						compiled = new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8);
					} else {
						Map<String,Object> data = new HashMap<>();
						data.put("src", jsx);
						Map<String,Object> map = new HashMap<>();
						map.put("data", data);
						JsxBundle.runner().run(JsxBundle.jsxCompiler(), map);
						compiled = data.get("code").toString();
						Files.write(cacheFile.toPath(), compiled.getBytes(StandardCharsets.UTF_8));
					}
					store.put(COMPILED, compiled);
				} catch (Exception e) {
					throw unchecked(e);
				}
			});
		});
		
		module.operation(root(), provider -> new JsxTemplateConfigurer());
	}
	
	private static String byteArrayToHexString(byte[] b) {
	  StringBuilder result = new StringBuilder();
	  for (int i = 0; i < b.length; i++) {
	    result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
	  }
	  return result.toString();
	}
	
	public static class JsxTemplateConfigurer implements OperationConfigurer {
		
		private final Function<Data,Path> outFn = data -> Response.CONTENT;

		@Override
		public void setup(OperationSetup ops) {
			ops.add("template", store -> new JsxTemplateOperation(store.get(COMPILED), outFn));
		}
		
	}
	
	public static class JsxTemplateOperation implements Operation {
		
		private static final Content APPLICATION_JAVASCRIPT = utf8("application/javascript");
		
		private final Content content;
		private final Function<Data,Path> outFn;
		
		public JsxTemplateOperation(String content, Function<Data,Path> outFn) {
			this.content = utf8(content);
			this.outFn = outFn;
		}

		@Override
		public void call(MutableData data) {
			Path out = outFn.apply(data);
			data.put(out, content);
			if (out.equals(Response.CONTENT)) {
				data.put(Response.Headers.CONTENT_TYPE, APPLICATION_JAVASCRIPT);
			}
		}
		
	}

}
