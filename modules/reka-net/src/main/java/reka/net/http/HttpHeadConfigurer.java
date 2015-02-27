package reka.net.http;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class HttpHeadConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("head", store -> new HttpHeadOperation());
	}

}
