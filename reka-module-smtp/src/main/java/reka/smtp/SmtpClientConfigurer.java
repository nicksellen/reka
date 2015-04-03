package reka.smtp;

import static reka.util.Path.path;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;

public class SmtpClientConfigurer extends ModuleConfigurer {
	
	private String host, username, password;
	private int port = 25;
	
	@Conf.At("host")
	public void host(String val) {
		host = val;
	}
	
	@Conf.At("port")
	public void port(String val) {
		port = Integer.valueOf(val);
	}
	
	@Conf.At("username")
	public void username(String val) {
		username = val;
	}
	
	@Conf.At("password")
	public void password(String val) {
		password = val;
	}

	@Override
	public void setup(AppSetup init) {
		init.defineOperation(path("send"), provider -> new SmtpSendConfigurer(host, port, username, password));
	}
	
}