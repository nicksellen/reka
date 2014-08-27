package reka.core.util;

import java.util.List;
import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;

public interface StringWithVars extends Function<Data,String> {
	
	static StringWithVars compile(String input) {
		return StringWithColonVariables.compile(input);
	}
	
	static boolean hasVars(String input) {
		return StringWithColonVariables.hasVars(input);
	}
	
	static StringWithVars compileWithAtVars(String input) {
		return StringWithAtVariables.compile(input);
	}
	
	static boolean hasAtVars(String input) {
		return StringWithAtVariables.hasVars(input);
	}
	
	List<Variable> vars();
	boolean hasVariables();
	String withPlaceholder(String val);
	
	public static interface Variable {
		
		Path path();
		
		default boolean hasDefaultValue() {
			return false;
		}
		
		default Object defaultValue() {
			return null;
		}
		
	}
	
}