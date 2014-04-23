package reka.validation;

import static reka.api.content.Contents.utf8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class ValidateOperation implements SyncOperation {

	private final List<ValidatorRule> rules = new ArrayList<>();
	private final Function<Data,Path> inFn, outFn;
	
	public ValidateOperation(List<ValidatorRule> rules, Function<Data,Path> inFn, Function<Data,Path> outFn) {
		this.rules.addAll(rules);
		this.inFn = inFn;
		this.outFn = outFn;
	}

	@Override
	public MutableData call(MutableData data) {
		
		DefaultValidatorErrors errors = new DefaultValidatorErrors();
		
		Data dataToValidate = data.at(inFn.apply(data));
		
		for (ValidatorRule rule : rules) {
			rule.validate(dataToValidate, errors);
		}
		
		Path out = outFn.apply(data);
		
		for (Entry<Path,List<String>> error : errors) {
			Path p = out.add(error.getKey().dots());
			for (String msg : error.getValue()) {
				data.putOrAppend(p, utf8(msg));
			}
		}
		
		return data;
	}
	
}