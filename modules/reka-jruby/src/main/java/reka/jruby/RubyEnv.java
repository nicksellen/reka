package reka.jruby;

import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import com.google.common.hash.Hashing;
import com.google.common.io.Resources;

public class RubyEnv {
	

	private final ScriptingContainer container;
	
	public static RubyEnv create(String gemFile) {
		return new RubyEnv(gemFile);
	}
	
	private RubyEnv(String gemFile) {
		
		container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.TRANSIENT);
		
		try {
			
			container.setClassLoader(container.getClass().getClassLoader());
			
			String gemFileHash = new String(Base64.getEncoder().encode(Hashing.sha1().newHasher().putString(gemFile, StandardCharsets.UTF_8).hash().asBytes()), StandardCharsets.UTF_8);

			File gemFileEnv = new File("/tmp/jrubyenv-" + gemFileHash);

			java.nio.file.Path tmpdir = Files.createTempDirectory("jruby");
			tmpdir.toFile().deleteOnExit();
			
			java.nio.file.Path gemhome = tmpdir.resolve("gemhome");
			java.nio.file.Path workhome = tmpdir.resolve("work");

			Map<String,String> env = new HashMap<>(System.getenv());
			
			env.put("GEM_PATH", gemhome.toFile().getAbsolutePath());
			
			container.setEnvironment(env);
			
			if (gemFileEnv.exists()) {
				System.out.printf("extracting %s into %s\n", gemFileEnv.getAbsolutePath(), tmpdir.toFile().getAbsolutePath());
				new ZipFile(gemFileEnv).extractAll(tmpdir.toFile().getAbsolutePath());
				container.setCurrentDirectory(workhome.toFile().getAbsolutePath());
			} else {
				Files.createDirectory(gemhome);
				Files.createDirectories(workhome);
				container.setCurrentDirectory(workhome.toFile().getAbsolutePath());
				Files.write(workhome.resolve("Gemfile"), gemFile.getBytes(StandardCharsets.UTF_8));
				execResource("/gem-install-bundler.rb");
				
				ZipFile zip = new ZipFile(gemFileEnv);
				ZipParameters params = new ZipParameters();
				params.setIncludeRootFolder(false);
				zip.createZipFileFromFolder(tmpdir.toFile(), params, false, 0);
			}

			execResource("/bundle-install.rb");
			execResource("/init-env.rb");
			
		} catch (IOException | ZipException e) {
			throw unchecked(e);
		}
	}
	
	public void exec(String code) {
		container.runScriptlet(code);
	}

	public ScriptingContainer container() {
		return container;
	}
	
	private void execResource(String name) {
		try {
			container.runScriptlet(Resources.toString(JRubyConfigurer.class.getResource(name), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

}
