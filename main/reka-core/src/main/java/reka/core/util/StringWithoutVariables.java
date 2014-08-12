package reka.core.util;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import reka.api.data.Data;

class StringWithoutVariables implements Function<Data,String>, StringWithVars {
	
	private final String val;
	
	public StringWithoutVariables(String val) {
		this.val = val;
	}

	@Override
	public String apply(Data data) {
		return val;
	}

	@Override
	public List<Variable> vars() {
		return Collections.emptyList();
	}

	@Override
	public boolean hasVariables() {
		return false;
	}

	@Override
	public String withPlaceholder(String placeholder) {
		return val;
	}
	
}