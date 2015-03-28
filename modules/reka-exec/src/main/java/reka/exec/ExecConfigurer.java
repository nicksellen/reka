package reka.exec;

import static reka.api.Path.root;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.AppSetup;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetupContext;

public class ExecConfigurer extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(ExecConfigurer.class);
	
	public static final IdentityKey<RekaSSHClient> CLIENT = IdentityKey.named("ssh client");
	
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
					
					RekaSSHClient existingClient = ctx.get(CLIENT);
					
					if (existingClient != null && !Arrays.equals(existingClient.sha1, ssh.sha1())) {
						log.info("not using old client because config has changed");
						ctx.remove(CLIENT);
					}
						
					ctx.calculateIfAbsent(CLIENT, () -> {

						try {
							
							RekaSSHClient client = new RekaSSHClient(ssh.sha1());
							
							client.version.set(app.version());
	
							log.info("creating new SSH client");
							
							client.addHostKeyVerifier(ssh.hostkey());
							client.useCompression();
							
							client.connect(ssh.hostname(), ssh.port());
							
							KeyProvider keyProvider = client.loadKeys(ssh.privateKeyAsString(), 
																	  ssh.publicKeyAsString(), 
																      PasswordUtils.createOneOff(ssh.passphrase()));
							
							client.authPublickey(ssh.user(), keyProvider);
							client.getConnection().getKeepAlive().setKeepAliveInterval(30);

							return client;
							
						} catch (Throwable t) {
							throw unchecked(t);
						}
							
					}).version.set(app.version());
					
				});
			});
			
			app.onUndeploy("disconnect ssh", () -> {
				ctx.lookup(CLIENT).ifPresent(client -> {
					if (client.version.get() != app.version()) {
						log.info("not disconnecting as we're reusing the client :)");
						return;
					}
					cleanupAndDisconnect(client);
				});
			});
			
			app.defineOperation(root(), provider -> new ExecSshCommandConfigurer(new ExecScripts(script, extraScripts)));
			
		} else {
			app.defineOperation(root(), provider -> new ExecCommandConfigurer(new ExecScripts(script, extraScripts), dirs().tmp()));
		}
		
	}
	
	private static void cleanupAndDisconnect(RekaSSHClient client) {
		try {
			log.info("disconnecting ssh client");
			client.beforeDisconnect.forEach(runnable -> {
				try {
					runnable.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			});
			
			client.disconnect();
		} catch (Exception e) {
			throw unchecked(e);
		}
	}
	
	public static class RekaSSHClient extends SSHClient {
		
		private final AtomicInteger version = new AtomicInteger(1);
		private final byte[] sha1;
		private final List<Runnable> beforeDisconnect = new ArrayList<>();
		
		public RekaSSHClient(byte[] configHash) {
			this.sha1 = configHash;
		}
		
		public void onBeforeDisconnect(Runnable runnable) {
			beforeDisconnect.add(runnable);
		}
		
	}

}
