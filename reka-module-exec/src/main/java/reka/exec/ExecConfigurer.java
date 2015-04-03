package reka.exec;

import static reka.api.Path.root;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.exec.ssh.ExecSshCommandConfigurer;
import reka.exec.ssh.RekaSshClient;
import reka.exec.ssh.SshConfig;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.module.setup.ModuleSetupContext;

public class ExecConfigurer extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(ExecConfigurer.class);
	
	public static final IdentityKey<RekaSshClient> CLIENT = IdentityKey.named("ssh client");
	public static final IdentityKey<String[]> COMMAND = IdentityKey.named("ssh command");
	
	private ByteBuffer script;
	private final Map<java.nio.file.Path,ByteBuffer> extraScripts = new HashMap<>();
	
	private SshConfig ssh;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = ByteBuffer.wrap(config.documentContent());
		}
	}
	
	@Conf.At("script")
	public void script(Config config) {
		if (config.hasDocument()) {
			script = ByteBuffer.wrap(config.documentContent());
		} else if (config.hasValue()) {
			script = ByteBuffer.wrap(config.valueAsString().getBytes(StandardCharsets.UTF_8));
		}
	}
	
	@Conf.EachChildOf("extra")
	public void extra(Config config) {
		checkConfig(config.hasDocument(), "extra scripts must be in a document");
		extraScripts.put(Paths.get(config.key()), ByteBuffer.wrap(config.documentContent()));
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
		private String hostkey;
		
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
			privateKey = configValOrDocChars(config);
		}
		
		@Conf.At("public-key")
		public void publicKey(Config config) {
			publicKey = configValOrDocChars(config);
		}
		
		@Conf.At("passphrase")
		public void passphrase(Config config) {
			passphrase = configValOrDocChars(config);
		}
		
		@Conf.At("hostkey")
		public void hostkey(String val) {
			hostkey = val;
		}
		
		public SshConfig build() {
			return new SshConfig(hostname, port, user, privateKey, publicKey, passphrase, hostkey);
		}
		
	}
	
	private static char[] configValOrDocChars(Config config) {
		checkConfig(config.hasDocument() || config.hasValue(), "must have document or value");
		if (config.hasDocument()) {
			return bytesToChars(config.documentContent());
		} else {
			return bytesToChars(config.valueAsString().getBytes(StandardCharsets.UTF_8));
		}
	}
	
	private static char[] bytesToChars(byte[] bytes) {
		return trim(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).array());
	}
	
	private static char[] trim(char[] value) {
		int len = value.length;
        int st = 0;
        char[] val = value;    

        while ((st < len) && (val[st] <= ' ')) {
            st++;
        }
        while ((st < len) && (val[len - 1] <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < value.length)) ? Arrays.copyOfRange(value, st, len) : value;
	}
	
	public static class ExecScripts {
		
		private final ByteBuffer script;
		private final Map<java.nio.file.Path,ByteBuffer> extraScripts;
		
		public ExecScripts(ByteBuffer script, Map<java.nio.file.Path,ByteBuffer> extraScripts) {
			this.script = script;
			this.extraScripts = extraScripts;
		}
		
		public ByteBuffer script() {
			return script;
		}
		
		public Map<java.nio.file.Path,ByteBuffer> extraScripts() {
			return extraScripts;
		}
		
	}
	
	@Override
	public void setup(AppSetup app) {
		
		ModuleSetupContext ctx = app.ctx();
		
		if (ssh != null) {
			
			app.onDeploy(init -> {
				init.run("ssh connect", () -> {
					
					RekaSshClient existingClient = ctx.get(CLIENT);
					
					if (existingClient != null && !Arrays.equals(existingClient.sha1(), ssh.sha1())) {
						log.info("not using old client because config has changed");
						ctx.remove(CLIENT);
					}
						
					ctx.calculateIfAbsent(CLIENT, () -> {

						try {
							
							RekaSshClient client = new RekaSshClient(ssh, 5);
							
							client.version(app.version());
							
							client.connect();

							return client;
							
						} catch (Throwable t) {
							throw unchecked(t);
						}
							
					}).version(app.version());
					
				});
				
				init.run("send scripts", () -> {
					ctx.put(COMMAND, ctx.require(CLIENT).sendScripts(new ExecScripts(script, extraScripts)));
				});
				
			});
			
			app.onUndeploy("disconnect ssh", () -> {
				ctx.lookup(CLIENT).ifPresent(client -> {
					if (client.version() == app.version()) {
						try {
							client.disconnect();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			});
			
			app.defineOperation(root(), provider -> new ExecSshCommandConfigurer());
			
		} else {
			
			app.onDeploy(init -> {
				init.run("write scripts", () -> {
					ctx.put(COMMAND, writeScriptsLocally(new ExecScripts(script, extraScripts), dirs().tmp()));
				});
			});
			
			app.defineOperation(root(), provider -> new ExecCommandConfigurer());
		}
		
	}

	private static String[] writeScriptsLocally(ExecScripts scripts, java.nio.file.Path tmp) {
		try {
			java.nio.file.Path dir = Files.createTempDirectory(tmp, "exec");
			java.nio.file.Path scriptPath = dir.resolve("__main__");
			
			writeByteBufferTo(scriptPath, scripts.script());
			
			scripts.extraScripts().forEach((path, buf) -> {
				try {
					writeByteBufferTo(dir.resolve(path), buf);
				} catch (Exception e) {
					throw unchecked(e);
				}
			});
			File scriptFile = scriptPath.toFile();
			scriptFile.setExecutable(true, true);
			return new String[] { scriptFile.getAbsolutePath() };
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	private static void writeByteBufferTo(java.nio.file.Path path, ByteBuffer buf) throws IOException {
		FileOutputStream fis = new FileOutputStream(path.toFile());
		FileChannel channel = fis.getChannel();
		channel.write(buf);
		channel.close();
		fis.close();
	} 

}
