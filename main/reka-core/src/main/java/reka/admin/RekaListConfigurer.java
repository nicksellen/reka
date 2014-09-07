package reka.admin;

import static reka.api.Path.dots;
import reka.ApplicationManager;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.nashorn.OperationsConfigurer;

public class RekaListConfigurer implements OperationsConfigurer {
	
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
		ops.add("list apps", store -> new RekaListOperation(manager, out));
	}

}
