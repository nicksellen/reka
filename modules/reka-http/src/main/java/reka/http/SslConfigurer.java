package reka.http;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.http.server.HttpSettings.SslSettings;

public class SslConfigurer {
	
	byte[] crt;
	byte[] key;
	
	@Conf.At("crt")
	public void crt(Config val) {
		checkConfig(val.hasDocument(), "must have document!");
		crt = val.documentContent();
	}
	
	@Conf.At("key")
	public void key(Config val) {
		checkConfig(val.hasDocument(), "must have document!");
		key = val.documentContent();
	}

	SslSettings build() {
		return new SslSettings(byteToFile(crt), byteToFile(key));
	}

	private static File byteToFile(byte[] bytes) {
		try {
			java.nio.file.Path tmp = Files.createTempFile("reka.", "");
			Files.write(tmp, bytes);
			File f = tmp.toFile();
			f.deleteOnExit();
			return f;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}