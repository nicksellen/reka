package reka.net.http.configurers;

import java.util.function.Function;

import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.net.http.operations.HttpRedirectOperation;
import reka.util.StringWithVars;

public class HttpRedirectConfigurer implements OperationConfigurer {

	private Function<Data,String> urlFn;
	private boolean temporary = true;
	
	@Conf.Val
	@Conf.At("url")
	public void url(String val) {
	    urlFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("redirect", () -> new HttpRedirectOperation(urlFn, temporary));
	}

}
