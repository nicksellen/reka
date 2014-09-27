package reka.smtp;

import static reka.api.Path.path;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.smtp.SMTPServerConfigurer.RekaSmtpServer;

public class SmtpModule implements Module {

	@Override
	public Path base() {
		return path("smtp");
	}
	
	private final Map<Integer,RekaSmtpServer> servers = new HashMap<>();

	public void setup(ModuleDefinition module) {
		module.main(() -> new SMTPClientConfigurer());
		module.submodule(path("server"), () -> new SMTPServerConfigurer(servers));
	}
	
}
