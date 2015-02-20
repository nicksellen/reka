package reka.exec;

import static java.lang.String.format;
import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;


public class ExecSshCommandOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String[] command;
	private final Path outInto, errInto, statusInto;
	private final int timeoutSeconds = 5;
	
	private final SSHClient ssh = new SSHClient();
	
	public ExecSshCommandOperation(String[] command, SshConfig config, Path into) {
		this.command = command;
		this.outInto = into.add("out");
		this.errInto = into.add("err");
		this.statusInto = into.add("status");
		try {
			ssh.loadKnownHosts();
			ssh.connect(config.hostname(), config.port());
			ssh.authPublickey(config.user());
		} catch (Throwable t) {
			t.printStackTrace();
			throw unchecked(t);
		}
	}
	
	@Override
	public void call(MutableData data, OperationResult res) {

		
		Reka.SharedExecutors.general.execute(() -> {
			
			try {
				
				final Session session = ssh.startSession();
				
		        try {
		        	final Command cmd = session.exec("hostname");

					ScheduledFuture<?> timeout = Reka.SharedExecutors.scheduled.schedule(() -> {
						try {
							cmd.close();
							session.close();
						} catch (Throwable t) {
							log.error("error closing channel", t);
						}
						res.error("timed out after %ds", timeoutSeconds);
					}, timeoutSeconds, TimeUnit.SECONDS);
		        
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
