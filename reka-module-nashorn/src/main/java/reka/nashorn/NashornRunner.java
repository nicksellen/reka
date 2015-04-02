package reka.nashorn;

import java.util.Map;

import javax.script.CompiledScript;

public interface NashornRunner {
	static final String REKA_OUTPUT_PROPERTY = "out";
	CompiledScript compile(String source);
	Object run(CompiledScript compiledScript, Map<String, Object> data);
}