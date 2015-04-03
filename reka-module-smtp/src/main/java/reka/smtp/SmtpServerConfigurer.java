package reka.smtp;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Path.path;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.flow.ops.Subscriber;
import reka.identity.IdentityKey;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.module.setup.ModuleSetupContext;
import reka.util.Path;

public class SmtpServerConfigurer extends ModuleConfigurer {

	private static final Logger log = LoggerFactory.getLogger(SmtpServerConfigurer.class);
	
	public static final IdentityKey<RekaSmtpServer> SERVER = IdentityKey.named("SMTP server");

	private final Map<Integer,RekaSmtpServer> servers;
	private final List<BiFunction<String,String,Boolean>> acceptors = new ArrayList<>();
	private ConfigBody emailHandler;
	private int port = 25;
	
	public SmtpServerConfigurer(Map<Integer,RekaSmtpServer> servers) {
		this.servers = servers;
	}

	@Conf.At("port")
	public void port(String val) {
		port = Integer.valueOf(val);
	}
	
	private static final Pattern REGEX_PATTERN = Pattern.compile("^/(.*)/$");
	
	@Conf.Each("from")
	public void from(String value) {
		Matcher m = REGEX_PATTERN.matcher(value);
		if (m.matches()) {
			acceptors.add(new FromPatternAcceptor(Pattern.compile(m.group(1))));
		} else {
			acceptors.add(new FromStringAcceptor(value));
		}
	}

	
	@Conf.Each("to")
	public void to(String value) {
		Matcher m = REGEX_PATTERN.matcher(value);
		if (m.matches()) {
			acceptors.add(new ToPatternAcceptor(Pattern.compile(m.group(1))));
		} else {
			acceptors.add(new ToStringAcceptor(value));
		}
	}
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		checkConfig(config.hasBody(), "must have a body");
		switch (config.valueAsString()) {
		case "email":
			emailHandler = config.body();
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
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

	private static File byteToFile(byte[] bytes) {
		try {
			java.nio.file.Path tmp = Files.createTempFile("reka.", "");
			Files.write(tmp, bytes);
			Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("r--------"));
			File f = tmp.toFile();
			f.deleteOnExit();
			return f;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public static class FromPatternAcceptor implements BiFunction<String,String,Boolean> {

		private final Pattern pattern;
		
		public FromPatternAcceptor(Pattern pattern) {
			this.pattern = pattern;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return pattern.matcher(from).find();
		}
		
	}
	
	public static class ToPatternAcceptor implements BiFunction<String,String,Boolean> {

		private final Pattern pattern;
		
		public ToPatternAcceptor(Pattern pattern) {
			this.pattern = pattern;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return pattern.matcher(to).find();
		}
		
	}
	

	
	public static class FromStringAcceptor implements BiFunction<String,String,Boolean> {

		private final String match;
		
		public FromStringAcceptor(String match) {
			this.match = match;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return match.equals(from);
		}
		
	}
	
	public static class ToStringAcceptor implements BiFunction<String,String,Boolean> {

		private final String match;
		
		public ToStringAcceptor(String match) {
			this.match = match;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return match.equals(to);
		}
		
	}
	
	private static final Path EMAIL_PATH = path("email");
	
	public class RekaSmtpServer implements Consumer<Data> {
		
		private final SMTPServer server;
		private final int port;
		private final EmailListener listener;
		
		private final Set<Flow> flows = new HashSet<>();
		
		public RekaSmtpServer(int port, Optional<SslSettings> tls) {
			this.port = port;
			this.listener = new EmailListener(this);
			if (tls.isPresent()) {
				server = new SecureSMTPServer(new SimpleMessageListenerAdapter(listener), tls.get());
			} else {
				server = new SMTPServer(new SimpleMessageListenerAdapter(listener));
				server.setHideTLS(true);
			}
			server.setPort(port);
		}
		
		public void setAcceptor(BiFunction<String,String,Boolean> acceptor) {
			listener.setAcceptor(acceptor);
		}
		
		private void start() {
			if (!server.isRunning()) {
				log.info("starting smtp server on port {}", port);
				server.start();
			}
		}
		
		public void stopIfEmpty() {
			if (server.isRunning() && flows.isEmpty()) {
				log.info("stopping smtp server on port {}", port);
				server.stop();
				servers.remove(port);
			}
		}
		
		public void shutdown() {
			flows.clear();
			server.stop();
			servers.remove(port);
		}
		
		public void add(Flow flow) {
			flows.add(flow);
			start();
		}
		
		public void remove(Flow flow) {
			flows.remove(flow);
			stopIfEmpty();
		}
		
		@Override
		public void accept(Data data) {
			flows.forEach(flow -> {
				flow.prepare()
				.mutableData(MutableMemoryData.create().put(EMAIL_PATH, data))
				.complete(new Subscriber(){

					@Override
					public void ok(MutableData data) {
						log.debug("ok!");
					}

					@Override
					public void halted() {
						log.debug("halted :(");
					}

					@Override
					public void error(Data data, Throwable t) {
						log.debug("error :(");
						t.printStackTrace();
					}
					
				}).run();
			});
		}
		
	}
	
	public static class SslSettings {
		
		private final File certChainFile;
		private final File keyFile;
		
		public SslSettings(File certChainFile, File keyFile) {
			this.certChainFile = certChainFile;
			this.keyFile = keyFile;
		}
		
		public File certChainFile() {
			return certChainFile;
		}
		
		public File keyFile() {
			return keyFile;
		}
		
	}
	
	public static class CombinedAcceptor implements BiFunction<String,String,Boolean> {

		private final Iterable<BiFunction<String,String,Boolean>> acceptors;
		
		public CombinedAcceptor(Iterable<BiFunction<String,String,Boolean>> acceptors) {
			this.acceptors = acceptors;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			for (BiFunction<String, String, Boolean> acceptor : acceptors) {
				if (acceptor.apply(from, to)) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	private static final BiFunction<String,String,Boolean> ACCEPT_ALL = (from, to) -> true;
	
	@Override
	public void setup(AppSetup app) {
		
		ModuleSetupContext ctx = app.ctx();
		
		if (emailHandler != null) {
			
			app.requireNetwork(port);
			
			app.onDeploy(init -> {
				init.run("start smtp server", () -> {
					
					RekaSmtpServer oldServer = servers.remove(port);
					if (oldServer != null) {
						oldServer.shutdown();
					}
					
					//RekaSmtpServer server = servers.computeIfAbsent(port, p -> new RekaSmtpServer(port));
					SslSettings tls = null;
					if (crt != null && key != null) {
						tls = new SslSettings(byteToFile(crt), byteToFile(key));
					}
					RekaSmtpServer server = new RekaSmtpServer(port, Optional.ofNullable(tls));
					if (acceptors.isEmpty()) {
						server.setAcceptor(ACCEPT_ALL);
					} else {
						server.setAcceptor(new CombinedAcceptor(acceptors));
					}
					server.start();
					ctx.put(SERVER, server);
					servers.put(port, server);
				});
			});
			
			app.buildFlow("on email", emailHandler, flow -> {
				RekaSmtpServer server = ctx.require(SERVER);
				server.add(flow);
				app.registerNetwork(port, "smtp");
				app.onUndeploy("undeploy smtp", () -> {
					server.remove(flow);
				});
			});
			
			app.onUndeploy("stop smtp server", () -> ctx.get(SERVER).stopIfEmpty());
			
		}
	}
	
	// http://blog.trifork.com/2009/11/10/securing-connections-with-tls/

	public static class SecureSMTPServer extends SMTPServer {
		
		private final SSLContext sslContext;
		private final SSLSocketFactory socketFactory;
		
		private final String[] PROTOCOLS;
		private final String[] CIPHERS;
		
		public SecureSMTPServer(MessageHandlerFactory handlerFactory, SslSettings tls) {
			super(handlerFactory);

		    sslContext = createSslContext(tls.keyFile(), tls.certChainFile());
		    
		    socketFactory = sslContext.getSocketFactory();
			setRequireTLS(true);
			SSLEngine engine = sslContext.createSSLEngine();

			Set<String> supportedProtocols = new HashSet<>(asList(engine.getSupportedProtocols()));
			Set<String> supportedCiphers = new HashSet<>(asList(engine.getSupportedCipherSuites()));
		    
		    List<String> protocols = desirableProtocols.stream().filter(s -> supportedProtocols.contains(s)).collect(toList());
		    List<String> ciphers = desirableCiphers.stream().filter(s -> supportedCiphers.contains(s)).collect(toList());
		    
		    PROTOCOLS = protocols.toArray(new String[protocols.size()]);
		    CIPHERS = ciphers.toArray(new String[ciphers.size()]);
		    
		    log.info("enabled protocols {}", asList(PROTOCOLS));
		    log.info("enabled ciphers {}", asList(CIPHERS));
		}
	
		
	  @Override
	  public SSLSocket createSSLSocket(Socket socket) throws IOException {
	    InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
	    SSLSocket s = (SSLSocket) (socketFactory.createSocket(socket, remoteAddress.getHostName(), socket.getPort(), true));
	    s.setUseClientMode(false);
	    s.setEnabledProtocols(PROTOCOLS);
	    s.setEnabledCipherSuites(CIPHERS);
	    s.getSSLParameters().setUseCipherSuitesOrder(true);
	    return s;
	  }
	}
	
	private static final List<String> desirableProtocols = asList(
		"TLSv1.2", "TLSv1.1", "TLSv1"
	);
	
	private static final List<String> desirableCiphers = asList(
		// XXX: Make sure to sync this list with OpenSslEngineFactory.
	    // GCM (Galois/Counter Mode) requires JDK 8.
	    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
	    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
	    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
	    // AES256 requires JCE unlimited strength jurisdiction policy files.
	    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
	    // GCM (Galois/Counter Mode) requires JDK 8.
	    "TLS_RSA_WITH_AES_128_GCM_SHA256",
	    "SSL_RSA_WITH_RC4_128_SHA",
	    "SSL_RSA_WITH_RC4_128_MD5",
	    "TLS_RSA_WITH_AES_128_CBC_SHA",
	    // AES256 requires JCE unlimited strength jurisdiction policy files.
	    "TLS_RSA_WITH_AES_256_CBC_SHA",
	    "SSL_RSA_WITH_DES_CBC_SHA"
	);
	
	public static SSLContext createSslContext(File keyFile, File certChainFile) {

		String keyPassword = "";
		String algorithm = "SunX509";
		
		try {
			
			KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            KeyFactory rsaKF = KeyFactory.getInstance("RSA");
            KeyFactory dsaKF = KeyFactory.getInstance("DSA");

            ByteBuf encodedKeyBuf = PemReader.readPrivateKey(keyFile);
            byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
            encodedKeyBuf.readBytes(encodedKey).release();

            char[] keyPasswordChars = keyPassword.toCharArray();
            PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPasswordChars, encodedKey);

            PrivateKey key;
            try {
                key = rsaKF.generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore) {
                key = dsaKF.generatePrivate(encodedKeySpec);
            }

            List<Certificate> certChain = new ArrayList<Certificate>();
            ByteBuf[] certs = PemReader.readCertificates(certChainFile);
            try {
                for (ByteBuf buf: certs) {
                    certChain.add(cf.generateCertificate(new ByteBufInputStream(buf)));
                }
            } finally {
                for (ByteBuf buf: certs) {
                    buf.release();
                }
            }

            ks.setKeyEntry("key", key, keyPasswordChars, certChain.toArray(new Certificate[certChain.size()]));

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, keyPasswordChars);
        	
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), null, null);
			
			return sslContext;
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}
	
	private static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
                   InvalidKeyException, InvalidAlgorithmParameterException {

        if (password == null || password.length == 0) {
            return new PKCS8EncodedKeySpec(key);
        }

        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

        Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }

	
}