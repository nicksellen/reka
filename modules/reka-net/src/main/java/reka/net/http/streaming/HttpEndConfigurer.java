package reka.net.http.streaming;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class HttpEndConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("end", ctx -> new HttpEndOperation());
	}

}
