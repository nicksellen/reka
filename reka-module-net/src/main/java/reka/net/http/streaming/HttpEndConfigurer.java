package reka.net.http.streaming;

import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;

public class HttpEndConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("end", () -> new HttpEndOperation());
	}

}
