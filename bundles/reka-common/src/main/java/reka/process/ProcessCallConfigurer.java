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
			ops.add("call (noreply)", store -> new ProcessCallNoreplyOperation(store.get(ProcessModule.PROCESS_MANAGER), lineFn));
		} else {
			ops.add("call", store -> new ProcessCallOperation(store.get(ProcessModule.PROCESS_MANAGER), lineFn));
		}
	}
	
}