package reka.exec;

import static reka.api.Path.root;
import static reka.config.configurer.Configurer.configure;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class ExecConfigurer extends ModuleConfigurer {
	
	private String[] command;
	private SshConfig ssh;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			setScript(config.documentContent());
		}
	}
	
	@Conf.At("script")
	public void script(Config config) {
		if (config.hasDocument()) {
			setScript(config.documentContent());
		} else if (config.hasValue()) {
			setScript(config.valueAsString().getBytes(StandardCharsets.UTF_8));
		}
	}
	
	@Conf.At("ssh")
	public void ssh(Config config) {
		ssh = configure(new ExecSshConfigurer(), config).build();
	}
	
	public static class ExecSshConfigurer {
		
		private String hostname;
		private int port = 22;
		private String user;
		private char[] privateKey, publicKey, passphrase;
		private final List<String> hostkeys = new ArrayList<>();
		
		@Conf.At("hostname")
		public void hostname(String val) {
			hostname = val;
		}
		
		@Conf.At("port")
		public void port(int val) {
			port = val;
		}
		
		@Conf.At("user")
		public void user(String val) {
			user = val;
		}
		
		@Conf.At("private-key")
		public void privateKey(Config config) {
			privateKey = bytesToChars(config.documentContent());
		}
		
		@Conf.At("public-key")
		public void publicKey(Config config) {
			publicKey = bytesToChars(config.documentContent());
		}
		
		@Conf.At("passphrase")
		public void passphrase(String val) {
			passphrase = bytesToChars(val.getBytes(StandardCharsets.UTF_8));
		}
		
		@Conf.Each("hostkey")
		public void hostkey(String val) {
			hostkeys.add(val);
		}
		
		private char[] bytesToChars(byte[] bytes) {
			return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).array();
		}
		
		public SshConfig build() {
			return new SshConfig(hostname, port, user, privateKey, publicKey, passphrase, hostkeys);
		}
		
	}
	
	private void setScript(byte[] script) {
		try {
			File file = Files.createTempFile(dirs().tmp(), "script.", "").toFile();
			Files.write(file.toPath(), script);
			file.deleteOnExit();
			file.setExecutable(true, true);
			command = new String[] { file.getAbsolutePath() };
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	@Override
	public void setup(ModuleSetup module) {
		if (ssh != null) {
			module.defineOperation(root(), provider -> new ExecSshCommandConfigurer(command, ssh));
		} else {
			module.defineOperation(root(), provider -> new ExecCommandConfigurer(command));
		}
	}

}
