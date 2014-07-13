package reka.admin;

import static reka.api.Path.dots;
import static reka.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.router;
import static reka.core.builder.FlowSegments.sequential;
import static reka.util.Util.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.util.StringWithVars;

public class RekaValidateConfigurer implements Supplier<FlowSegment> {

	private final ConfigurerProvider provider;
	private final ApplicationManager manager;
	
	private Path in;
	private Function<Data,String> filenameFn;
	
	private Supplier<FlowSegment> whenOk;
	private Supplier<FlowSegment> whenError;
	
	RekaValidateConfigurer(ConfigurerProvider provider, ApplicationManager manager) {
		this.provider = provider;
		this.manager = manager;
	}
	
	@Conf.At("in")
	public void in(String val) {
		in = dots(val);
	}
	
	@Conf.At("filename")
	public void filename(String val) {
		filenameFn = StringWithVars.compile(val);
	}

	@Conf.Each("when")
	public void when(Config config) {
		switch (config.valueAsString()) {
		case "ok":
			whenOk = ops(config.body());
			break;
		case "error":
			whenError = ops(config.body());
			break;
		default:
			throw runtime("no when case for %s", config.valueAsString());
		}
	}

	
	private Supplier<FlowSegment> ops(ConfigBody body) {
		return configure(new SequenceConfigurer(provider), body);
	}
	
	@Override
	public FlowSegment get() {
		
		FlowSegment router;
		
		if (in != null) {
			router = router("validate", (data) -> new RekaValidateFromContentOperation(manager, in));
		} else if (filenameFn != null) {
			router = router("validate", (data) -> new RekaValidateFromFileOperation(manager, filenameFn));
		} else {
			throw runtime("must specify either 'in' or 'filename'");
		}
		
		return sequential(seq -> {
			seq.add(router);
			seq.parallel(par -> {
				if (whenOk != null) par.add("ok", whenOk.get());
				if (whenError != null) par.add("error", whenError.get());
			});
		});
	}
	
}