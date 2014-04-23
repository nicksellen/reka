package reka.jruby;

import static org.jruby.javasupport.JavaEmbedUtils.rubyToJava;

import java.util.Map;

import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.data.memory.MutableMemoryData;

public class JRubyRunOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final EmbedEvalUnit script;
	private final Path out;
	
	public JRubyRunOperation(ScriptingContainer ruby, String script, Path out) {
		this.script = ruby.parse(script);
		this.out = out;
	}

	@Override
	public MutableData call(MutableData data) {
		Object result = rubyToJava(script.run());
		if (result == null) {
			log.debug("jruby return null\n");
		} else if (result instanceof String) {
			data.putString(out, (String) result);
		} else if (result instanceof Map) {
			
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>) result;
			
			data.put(out, MutableMemoryData.createFromMap(map));
		} else {
			log.debug("jruby return ({}) [{}]\n", result.getClass(), result);
		}
		return data;
	}
	
}