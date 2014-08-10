package reka.http.configurers;

import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;
import reka.http.operations.HttpRedirectOperation;

public class HttpRedirectConfigurer implements Supplier<FlowSegment> {

	private Function<Data,String> urlFn;
	private boolean temporary = true;
	
	@Conf.Val
	@Conf.At("url")
	public void url(String val) {
	    urlFn = StringWithVars.compile(val);
	}
	
	@Override
	public FlowSegment get() {
		return sync("http/redirect", () -> new HttpRedirectOperation(urlFn, temporary));
	}

}
