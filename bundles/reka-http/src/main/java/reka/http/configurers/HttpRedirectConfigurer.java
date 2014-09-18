package reka.http.configurers;

import java.util.function.Function;

import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.http.operations.HttpRedirectOperation;
import reka.nashorn.OperationConfigurer;

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
		ops.add("redirect", store -> new HttpRedirectOperation(urlFn, temporary));
	}

}
