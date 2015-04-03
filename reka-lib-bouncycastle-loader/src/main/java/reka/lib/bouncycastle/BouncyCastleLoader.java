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
	
	private static final Logger log = LoggerFactory.getLogger(BouncyCastleLoader.class);
	
	private static final String CLASS_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider";
	
	public static Provider createSecurityProvider() throws Throwable {
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
	
	public static ClassLoader createClassLoader(ClassLoader parent) {
		try {
			Path tmpdir = Files.createTempDirectory("bc");
			URL bcpkix = toURL("/bcpkix-jdk15on-151.jar", tmpdir.resolve("bcpkix.jar"));
			URL bcprov = toURL("/bcprov-jdk15on-151.jar", tmpdir.resolve("bcprov.jar"));
			return new URLClassLoader(new URL[] { bcpkix, bcprov }, parent);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static URL toURL(String name, Path to) throws IOException {
		Files.copy(BouncyCastleLoader.class.getResourceAsStream(name), to);
		return to.toUri().toURL();
	}
	
}
