package reka.net.http;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class HttpWriteConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("write", store -> new HttpWriteOperation());
	}

}
