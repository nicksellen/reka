package reka.builtins;

import static java.util.Collections.reverseOrder;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.parser.values.KeyAndSubkey;
import reka.config.processor.ConfigConverter;
import reka.config.processor.Processor;
import reka.core.data.memory.MutableMemoryData;
import reka.core.util.StringWithVars;

import com.google.common.base.Charsets;

public final class EachConverter implements ConfigConverter {

	private static final Pattern KEY_INCLUDE = Pattern.compile("^@(reverse\\-)?each(?:\\(([^\\s\\(\\)]*)\\))?$");
	
	private static final Pattern EXTENSION = Pattern.compile("^(.+)\\.([^\\.]+)$");
	
    @Override
    public void convert(Config config, Output out) {
    	
    	if (config.hasKey() && config.hasBody()) {
    		Matcher m = KEY_INCLUDE.matcher(config.key());
    		if (m.matches()) {
    			boolean reverse = m.group(1) != null;
    			String path = m.group(2);
    			checkConfig(config.source().supportsNestedFile(), "cannot use each as we don't support nested files here");
    			
    			List<Path> files = config.source().nestedFiles(path);
    			
    			if (reverse) files.sort(reverseOrder());
    			
    			for (Path file : files) {

    				String basename = file.getFileName().toString();
    				
    				Path parent = file.getParent();
    				
    				MutableData data = MutableMemoryData.create()
    						.putString("basename", basename)
    						.putString("absolute", file.toAbsolutePath().toString())
    						.putString("dirname", parent != null ? parent.toString() : "")
    						.putString("path", file.toString());
    				    				
    				Matcher matcher = EXTENSION.matcher(basename);
    				if (matcher.matches()) {
    					data.putString("filename", matcher.group(1));
    					data.putString("extension", matcher.group(2));
    				} else{
    					data.putString("filename", basename);
    					data.putString("extension", "");
    				}
    				
    				out.add(new Processor(new VarReplaceConverter(data.immutable())).process(config.body()));
    				
    			}
    			
    			return;
    		}
    	}

		out.add(config);
		
    }
    
    public static class VarReplaceConverter implements ConfigConverter {
    	
    	private final Data data;
    	
    	public VarReplaceConverter(Data data) {
    		this.data = data;
		}

		@Override
		public void convert(Config config, Output out) {
			String key = null;
			String subkey = null;
			String value = null;
			if (config.hasKey()) key = replaceVars(config.key());
			if (config.hasSubkey()) subkey = replaceVars(config.subkey());
			if (config.hasValue()) value = replaceVars(config.valueAsString());
			if (config.hasDocument()) {
				String docType = replaceVars(config.documentType());
				byte[] docContent = replaceVars(new String(config.documentContentAsString())).getBytes(Charsets.UTF_8);
				out.doc(new KeyAndSubkey(key, subkey), value, docType, docContent);
			} else if (config.hasBody()) {
				out.obj(new KeyAndSubkey(key, subkey), value, config.body());
			} else if (config.hasValue()){
				out.keyvalue(new KeyAndSubkey(key, subkey), value);
			} else {
				out.key(new KeyAndSubkey(key, subkey));
			}
		}
		
		private final String replaceVars(String input) {
			if (StringWithVars.hasAtVars(input)) {
				return StringWithVars.compileWithAtVars(input).apply(data);
			} else {
				return input;
			}
		}
    	
    }

}