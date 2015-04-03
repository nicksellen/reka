package reka.smtp;

import static reka.api.Path.path;

import java.util.HashMap;
import java.util.Map;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Multipart;

import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.smtp.SmtpServerConfigurer.RekaSmtpServer;

public class SmtpModule implements Module {

	@Override
	public Path base() {
		return path("smtp");
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
		module.main(() -> new SmtpClientConfigurer());
		module.submodule(path("server"), () -> new SmtpServerConfigurer(servers));
	}
	
}
