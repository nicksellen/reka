package reka.admin;

import static reka.api.Path.dots;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.app.manager.ApplicationManager;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class RekaListConfigurer implements OperationConfigurer {
	
	private final ApplicationManager manager;
	
	private Path out = dots("apps");
	
	@Conf.Val
	public void out(String val) {
		out = dots(val);
	}
	
	public RekaListConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("list", () -> new RekaListOperation(manager, out));
	}

}
