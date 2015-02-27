package reka.net.http;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class HttpEndConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("end", store -> new HttpEndOperation());
	}

}
