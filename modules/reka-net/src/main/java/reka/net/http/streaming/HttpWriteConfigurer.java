package reka.net.http.streaming;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class HttpWriteConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("write", ctx -> new HttpWriteOperation());
	}

}
