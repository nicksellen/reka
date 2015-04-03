package reka.process;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class ProcessCallConfigurer implements OperationConfigurer {

	private final boolean noreply;
	
	private Function<Data,String> lineFn = (data) -> data.toJson();
	
	public ProcessCallConfigurer(boolean noreply) {
		this.noreply = noreply;
	}
	
	@Conf.Val
	public void line(String val) {
		lineFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		if (noreply) {
			ops.add("call (noreply)", () -> new ProcessCallNoreplyOperation(ops.ctx().get(ProcessConfigurer.PROCESS_MANAGER), lineFn));
		} else {
			ops.add("call", () -> new ProcessCallOperation(ops.ctx().get(ProcessConfigurer.PROCESS_MANAGER), lineFn));
		}
	}
	
}