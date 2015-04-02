package reka.exec;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationContext;
import reka.exec.ssh.RekaSshClient;

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
