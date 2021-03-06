package reka.modules.admin;

import static reka.util.Path.dots;
import reka.app.manager.ApplicationManager;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

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
