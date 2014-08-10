package reka.validation;

import static reka.api.content.Contents.utf8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;

public class RoutingValidateOperation implements RoutingOperation {

	private final List<ValidatorRule> rules = new ArrayList<>();
	private final Function<Data,Path> inFn, outFn;
	
	public RoutingValidateOperation(List<ValidatorRule> rules, Function<Data,Path> inFn, Function<Data,Path> outFn) {
		this.rules.addAll(rules);
		this.inFn = inFn;
		this.outFn = outFn;
	}
	
	@Override
	public MutableData call(MutableData data, RouteCollector router) {
		
		DefaultValidatorErrors errors = new DefaultValidatorErrors();
		
		Data dataToValidate = data.at(inFn.apply(data));
		
		for (ValidatorRule rule : rules) {
			rule.validate(dataToValidate, errors);
		}
		
		Path out = outFn.apply(data);
		
		boolean valid = true;
		
		for (Entry<Path,List<String>> error : errors) {
			valid = false;
			Path p = out.add(error.getKey().dots());
			int i = 0;
			for (String msg : error.getValue()) {
				data.put(p.add(i++), utf8(msg));
			}
		}
		
		if (valid) {
			router.routeTo("valid");
		} else {
			router.routeTo("invalid");
		}
		
		return data;
	}

}
