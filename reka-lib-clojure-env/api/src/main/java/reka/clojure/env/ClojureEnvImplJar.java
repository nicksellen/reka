package reka.clojure.env;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ClojureEnvImplJar {

	private final URL jar;
	
	private static ClojureEnvImplJar INSTANCE = new ClojureEnvImplJar();
	
	public static URL url() {
		return INSTANCE.jar;
	}

	private ClojureEnvImplJar() {
		try {
			InputStream s = ClojureEnv.class.getClassLoader().getResourceAsStream("clojure-env-impl.jar");
			Path tmp = Files.createTempFile("clojure-env-impl", ".jar");
			Files.copy(s, tmp, StandardCopyOption.REPLACE_EXISTING);
			tmp.toFile().deleteOnExit();
			jar = tmp.toUri().toURL();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}