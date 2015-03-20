package reka.net.http.streaming;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class HttpHeadConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("head", ctx -> new HttpHeadOperation());
	}

}
