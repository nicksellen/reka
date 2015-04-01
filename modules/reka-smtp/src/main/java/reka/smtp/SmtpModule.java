package reka.smtp;

import static reka.api.Path.path;

import java.util.HashMap;
import java.util.Map;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.smtp.SmtpServerConfigurer.RekaSmtpServer;

public class SmtpModule implements Module {

	@Override
	public Path base() {
		return path("smtp");
	}
	
	private final Map<Integer,RekaSmtpServer> servers = new HashMap<>();

	public void setup(ModuleDefinition module) {
		module.main(() -> new SmtpClientConfigurer());
		module.submodule(path("server"), () -> new SmtpServerConfigurer(servers));
	}
	
}
