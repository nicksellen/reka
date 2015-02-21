package reka.exec;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;


public class ExecSshCommandOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Path outInto, errInto, statusInto;
	private final int timeoutSeconds = 5;
	
	private final SSHClient ssh = new SSHClient();
	
	public ExecSshCommandOperation(String[] command, SshConfig config, Path into) {
		this.outInto = into.add("out");
		this.errInto = into.add("err");
		this.statusInto = into.add("status");
		try {
			ssh.addHostKeyVerifier("b5:63:de:5c:ef:a9:5a:a2:7a:c7:51:55:d3:85:72:d3");
			ssh.connect(config.hostname(), config.port());
			
			KeyProvider keyProvider = ssh.loadKeys(config.privateKeyAsString(), 
												   config.publicKeyAsString(), 
												   PasswordUtils.createOneOff(config.passphrase()));
			
			ssh.authPublickey(config.user(), keyProvider);
			ssh.getConnection().getKeepAlive().setKeepAliveInterval(30);
		} catch (Throwable t) {
			t.printStackTrace();
			throw unchecked(t);
		}
	}
	
	@Override
	public void call(MutableData data, OperationResult res) {

		
		Reka.SharedExecutors.general.execute(() -> {
			final AtomicReference<Session> sessionRef = new AtomicReference<>();
			final AtomicReference<Command> commandRef = new AtomicReference<>();
			
			ScheduledFuture<?> timeout = Reka.SharedExecutors.scheduled.schedule(() -> {
				try {
					Command cmd = commandRef.get();
					if (cmd != null) {
						cmd.close();	
					}
					Session session = sessionRef.get();
					if (session != null) {
						session.close();
					}
				} catch (Throwable t) {
					log.error("error closing channel", t);
				}
				res.error("timed out after %ds", timeoutSeconds);
			}, timeoutSeconds, TimeUnit.SECONDS);
			
			try {
				final Session session;
				
				synchronized (ssh) {	
					session = ssh.startSession();
					sessionRef.set(session);
				}
				
		        try {
		        	
		        	final Command cmd = session.exec("ifconfig -a");
		        	commandRef.set(cmd);
		        
		        	InputStream err = cmd.getErrorStream();
					InputStream out = cmd.getInputStream();
					
					ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
					ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
					
					while (cmd.getExitStatus() == null || err.available() > 0 || out.available() > 0) {
						if (err.available() > 0) {
							errBytes.write(err.read());
						}
						if (out.available() > 0) {
							outBytes.write(out.read());
						}
					}
					
					if (!timeout.isDone()) {

						timeout.cancel(true);
						
						data.putInt(statusInto, cmd.getExitStatus());
						data.putString(outInto, new String(outBytes.toByteArray(), StandardCharsets.UTF_8));
						data.putString(errInto, new String(errBytes.toByteArray(), StandardCharsets.UTF_8));

						res.done();

					}
					
		        } finally {
		            session.close();
		        }
		        
			} catch (Throwable t) {
				res.error(t);
			}
		});
		
	}

}
