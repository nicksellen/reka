package reka.jruby;

import static java.lang.String.format;

import java.util.Map;
import java.util.UUID;

import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.data.memory.MutableMemoryData;

public class JRubyRunOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ScriptingContainer ruby;
	//private final EmbedEvalUnit script;
	private final Path out;
	
	private final String methodName;
	
	public JRubyRunOperation(ScriptingContainer ruby, String script, Path out) {
		this.ruby = ruby;
		String uniqueName = "reka_" + UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "");
		this.methodName = uniqueName;
		ruby.runScriptlet(format("def %s(data)\ndata = DataWrapper.new(data)\n%s\nend\n", methodName, script));
		this.out = out;
	}

	@Override
	public MutableData call(MutableData data) {
		
		Object result = ruby.callMethod(null, methodName, data, Object.class);
		
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