package reka.jruby;

import static java.lang.String.format;

import java.util.Map;
import java.util.UUID;

import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;

public class JRubyRunOperation implements Operation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ScriptingContainer container;
	private final Path out;
	
	private final String methodName;
	
	public JRubyRunOperation(RubyEnv ruby, String script, Path out) {
		this.container = ruby.container();
		String uniqueName = "reka_" + UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "");
		this.methodName = uniqueName;
		ruby.exec(format("def %s(data)\ndata = DataWrapper.new(data)\n%s\nend\n", methodName, script));
		this.out = out;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		
		Object result = container.callMethod(null, methodName, data, Object.class);
		
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
	}
	
}