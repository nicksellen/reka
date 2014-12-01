package reka.net.http;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.net.NetSettings.SslSettings;

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

	public SslSettings build() {
		return new SslSettings(byteToFile(crt), byteToFile(key));
	}

	private static File byteToFile(byte[] bytes) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString("r--------");
			java.nio.file.Path tmp = Files.createTempFile("reka.", "", PosixFilePermissions.asFileAttribute(perms));
			Files.write(tmp, bytes);
			File f = tmp.toFile();
			f.deleteOnExit();
			return f;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}