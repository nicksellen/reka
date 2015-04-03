package reka.crypto.bcrypt;

import org.mindrot.jbcrypt.BCrypt;

import reka.api.Path;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class BCryptHashpwOperation implements Operation {

	private final Path in;
	private final Path out;

	public BCryptHashpwOperation(Path in, Path out) {
		this.in = in;
		this.out = out;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.getContent(in).ifPresent(content -> {
			data.putString(out, BCrypt.hashpw(content.asUTF8(), BCrypt.gensalt()));
		});
	}
	
}