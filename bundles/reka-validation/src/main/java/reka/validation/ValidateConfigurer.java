package reka.validation;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.sequential;
import static reka.core.builder.FlowSegments.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.util.StringWithVars;

public class ValidateConfigurer implements Supplier<FlowSegment> {

	private final ConfigurerProvider provider;

	private final List<ValidatorRule> rules;
	
	private List<Supplier<FlowSegment>> whenValid = new ArrayList<>(), whenInvalid = new ArrayList<>();
	
	private Function<Data,Path> outFn = (unused) -> path("errors");
	private Function<Data,Path> inFn = (unused) -> root();
	
	public ValidateConfigurer(ConfigurerProvider provider, List<ValidatorRule> rules) {
		this.provider = provider;
		this.rules = rules;
	}
	
	@Conf.At("in")
	@Conf.Val
	public void in(String val) {
		inFn = StringWithVars.compile(val).andThen(s -> dots(s));
	}
	
	@Conf.At("out")
	public void out(String val) {
		outFn = StringWithVars.compile(val).andThen(s -> dots(s));
	}
	
	@Conf.Each("when")
	public void when(Config config) {
		switch (config.valueAsString()) {
		case "valid":
			whenValid.add(ops(config.body()));
			break;
		case "invalid":
			whenInvalid.add(ops(config.body()));
			break;
		}
	}

	@Override
	public FlowSegment get() {
		
		if (whenValid.isEmpty() && whenInvalid.isEmpty()) {
			return sync("validate", () -> new ValidateOperation(rules, inFn, outFn));
		} else {
		
			return sequential(seq -> {
				seq.routerNode("validate(routed)", (unused) -> new RoutingValidateOperation(rules, inFn, outFn));
				seq.parallel(par -> {
					for (Supplier<FlowSegment> item : whenValid) {
						par.add("valid", item.get());
					}
					for (Supplier<FlowSegment> item : whenInvalid) {
						par.add("invalid", item.get());
					}
				});
			});
		}
	}
	
	private Supplier<FlowSegment> ops(ConfigBody body) {
		return configure(new SequenceConfigurer(provider), body);
	}
	
}