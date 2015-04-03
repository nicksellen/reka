package reka.lib.bouncycastle;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BouncyCastleLoader {
	
	private static final BouncyCastleLoader INSTANCE = new BouncyCastleLoader();
	
	private static final Logger log = LoggerFactory.getLogger(BouncyCastleLoader.class);
	
	private static final String CLASS_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider";
	
	public static Provider createSecurityProvider() throws Throwable {
		return INSTANCE.newSecurityProvider();
	}
	
	public static ClassLoader createClassLoader(ClassLoader parent) {
		return INSTANCE.newClassLoader(parent);
	}
	
	private final Path tmpdir, bcpkixPath, bcprovPath;
	private final URL bcpkix, bcprov;
	
	private BouncyCastleLoader() {
		try {
			this.tmpdir = Files.createTempDirectory("reka.bc.");
			
			this.bcpkixPath = tmpdir.resolve("bcpkix.jar");
			this.bcprovPath = tmpdir.resolve("bcprov.jar");
	
			this.bcpkix = writeToFile("/bcpkix-jdk15on-151.jar", bcpkixPath);
			this.bcprov = writeToFile("/bcprov-jdk15on-151.jar", bcprovPath);
			
		} catch (Throwable t) {
			log.error("failed to initialize " + BouncyCastleLoader.class.getName(), t);
			throw new RuntimeException(t);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override public void run() {
				try {
					Files.deleteIfExists(bcpkixPath);
					Files.deleteIfExists(bcprovPath);
					Files.deleteIfExists(tmpdir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
	}
	
	private ClassLoader newClassLoader(ClassLoader parent) {
		return new URLClassLoader(new URL[] { bcpkix, bcprov }, parent);
	}

	private Provider newSecurityProvider() throws Throwable {
		try {
			ClassLoader cl = createClassLoader(BouncyCastleLoader.class.getClassLoader());
			Class<?> klass = Class.forName(CLASS_NAME, true, cl);
			Provider provider = (Provider)klass.newInstance();
			java.security.Security.addProvider(provider);
			return provider;
		} catch (Throwable t) {
			log.error("failed to load " + CLASS_NAME, t);
			throw t;
		}
	}

	private static URL writeToFile(String name, Path to) throws IOException {
		Files.copy(BouncyCastleLoader.class.getResourceAsStream(name), to);
		return to.toUri().toURL();
	}
	
}
