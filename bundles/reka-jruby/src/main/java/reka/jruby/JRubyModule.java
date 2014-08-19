package reka.jruby;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;

public class JRubyModule extends ModuleConfigurer {
	
	public static ScriptingContainer createContainer(String gemFile) {
		ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.TRANSIENT);
		try {
			
			container.setClassLoader(container.getClass().getClassLoader());
			
			String gemFileHash = new String(Base64.getEncoder().encode(Hashing.sha1().newHasher().putString(gemFile).hash().asBytes()), Charsets.UTF_8);

			File gemFileEnv = new File("/tmp/jrubyenv-" + gemFileHash);

			java.nio.file.Path tmpdir = Files.createTempDirectory("jruby");
			tmpdir.toFile().deleteOnExit();
			
			java.nio.file.Path gemhome = tmpdir.resolve("gemhome");
			java.nio.file.Path workhome = tmpdir.resolve("work");

			Map<String,String> env = new HashMap<>(System.getenv());
			
			env.put("GEM_PATH", gemhome.toFile().getAbsolutePath());
			env.put("WORK_HOME", workhome.toFile().getAbsolutePath());
			
			container.setEnvironment(env);
			
			if (gemFileEnv.exists()) {
				System.out.printf("extracting %s into %s\n", gemFileEnv.getAbsolutePath(), tmpdir.toFile().getAbsolutePath());
				new ZipFile(gemFileEnv).extractAll(tmpdir.toFile().getAbsolutePath());
				
				container.getProvider().getRubyInstanceConfig().setCurrentDirectory(workhome.toFile().getAbsolutePath());
				container.setCurrentDirectory(workhome.toFile().getAbsolutePath());
				
				System.out.printf("re-running bundle install\n");
				
				String initBundler = Resources.toString(JRubyModule.class.getResource("/reinit-bundler.rb"), Charsets.UTF_8);
				container.runScriptlet(initBundler);
				
				System.out.printf("bundle install complete\n");
				
			} else {
				Files.createDirectory(gemhome);
				Files.createDirectories(workhome);
				
				container.getProvider().getRubyInstanceConfig().setCurrentDirectory(workhome.toFile().getAbsolutePath());
				container.setCurrentDirectory(workhome.toFile().getAbsolutePath());
				
				Files.write(workhome.resolve("Gemfile"), gemFile.getBytes(Charsets.UTF_8));

				String initBundler = Resources.toString(JRubyModule.class.getResource("/init-bundler.rb"), Charsets.UTF_8);
				container.runScriptlet(initBundler);
		
				ZipFile zip = new ZipFile(gemFileEnv);
				ZipParameters params = new ZipParameters();
				params.setIncludeRootFolder(false);
				zip.createZipFileFromFolder(tmpdir.toFile(), params, false, 0);
			}

			String initEnv = Resources.toString(JRubyModule.class.getResource("/init.rb"), Charsets.UTF_8);
			container.runScriptlet(initEnv);
		} catch (IOException | ZipException e) {
			throw unchecked(e);
		}
		return container;
	}

	private String script;
	private String gemFile;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Conf.At("init")
	public void init(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Conf.At("Gemfile")
	public void gemfile(Config config) {
		checkConfig(config.hasDocument(), "must have document");
		gemFile = config.documentContentAsString();
	}
	
	@Override
	public void setup(ModuleInit module) {
		
		Path runtimePath = module.path().add("runtime");
		
		module.init("initialize runtime", (data) -> {
			ScriptingContainer container = createContainer(gemFile);
			container.runScriptlet(script);
			data.put(runtimePath, nonSerializableContent(container));
			return data;
		});
		
		module.operation(asList(path("run"), root()), () -> new JRubyRunConfigurer(runtimePath, module.path()));
	}
	
}