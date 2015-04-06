package reka.email;

import static reka.util.Path.root;
import static reka.util.Path.slashes;

import java.util.HashMap;
import java.util.Map;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Multipart;

import reka.email.imap.ImapConfigurer;
import reka.email.smtp.SmtpClientConfigurer;
import reka.email.smtp.SmtpServerConfigurer;
import reka.email.smtp.SmtpServerConfigurer.RekaSmtpServer;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

public class EmailModule implements Module {

	@Override
	public Path base() {
		return root();
	}
	
	static {
		try {
			CommandMap.setDefaultCommandMap(new MailcapCommandMap(Multipart.class.getResourceAsStream("/META-INF/mailcap")));
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private final Map<Integer,RekaSmtpServer> servers = new HashMap<>();

	public void setup(ModuleDefinition module) {
		module.submodule(slashes("smtp"), () -> new SmtpClientConfigurer());
		module.submodule(slashes("smtp/server"), () -> new SmtpServerConfigurer(servers));
		module.submodule(slashes("imap"), () -> new ImapConfigurer());
	}
	
}
