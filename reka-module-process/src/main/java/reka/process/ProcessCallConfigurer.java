package reka.process;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

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