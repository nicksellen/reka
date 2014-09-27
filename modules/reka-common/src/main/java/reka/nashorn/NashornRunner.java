package reka.nashorn;

import java.util.Map;

import javax.script.CompiledScript;

public interface NashornRunner {
	CompiledScript compile(String source);
	Map<String,Object> run(CompiledScript compiledScript, Map<String, Object> data);
}