package reka.net.http.streaming;

import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;

public class HttpWriteConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("write", () -> new HttpWriteOperation());
	}

}
