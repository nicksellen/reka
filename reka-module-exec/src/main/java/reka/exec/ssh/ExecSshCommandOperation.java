package reka.exec.ssh;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;

import reka.data.MutableData;
import reka.flow.ops.AsyncOperation;
import reka.flow.ops.OperationContext;
import reka.util.Path;

public class ExecSshCommandOperation implements AsyncOperation {

	private final String command;
	private final Path outInto, errInto, statusInto;
	
	private final RekaSshClient ssh;
	
	public ExecSshCommandOperation(String[] command, RekaSshClient ssh, Path into) {
		checkArgument(command.length == 1, "ssh command must be of length 1, not %d", command.length);
		this.outInto = into.add("out");
		this.errInto = into.add("err");
		this.statusInto = into.add("status");
		this.ssh = ssh;
		this.command = command[0];
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {

		Map<String,String> env = new HashMap<>();
		
		data.forEachContent((path, content) -> {
			String key = path.join("__").toUpperCase().replaceAll("[^A-Z0-9]", "_");
			String val = content.toString();
			env.put(key, val);
		});
		
		ssh.exec(command, env, 3).whenComplete((result, ex) ->  {
			if (ex != null) {
				res.error(ex);
				return;
			}
			data.putInt(statusInto, result.status);
			data.putString(outInto, result.out);
			data.putString(errInto, result.err);
			res.done();
		});
		
	}
	
}
