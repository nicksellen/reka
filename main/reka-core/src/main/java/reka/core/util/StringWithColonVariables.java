package reka.core.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static reka.api.Path.dots;
import static reka.api.content.Contents.utf8;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.collect.ImmutableList;

class StringWithColonVariables implements Function<Data,String>, StringWithVars {

	private static final Logger log = LoggerFactory.getLogger(StringWithColonVariables.class);
	
	//private static final Pattern pattern = Pattern.compile(":([^:\\s,()]+[^:\\s,().])");
	private static final Pattern pattern = Pattern.compile(":\\{?([a-zA-Z0-9-_\\.\\[\\]]+\\b)\\}?");
	
	public static void main(String[] args) throws Exception {
		
		Function<Data,String> v = StringWithVars
				.compile("this :special.thing should match :person.name [:person.name] :{person.name}oh yay with no match [:nothing.to.match] :)");
		
		MutableData data = MutableMemoryData.create();
		data.put(dots("special.thing"), utf8("heya!"));
		data.put(dots("person.name"), utf8("a name!"));
		
		String out = v.apply(data);
		log.debug("formatted: {}\n", out);
		
		log.debug("no variables [{}]\n", StringWithVars.compile("a nice normal string").apply(data));
	}
	

	public static class ColonVariable implements Variable {
		
		private final Path path;
		private final int pos;
		
		ColonVariable(Path path, int pos) {
			this.path = path;
			this.pos = pos;
		}
		
		public Path path() {
			return path;
		}
		
	}
	
	public static StringWithVars compile(String input) {
		return build(input);
	}
	
	private final List<Variable> entries;
	private final String base;
	private final String original;
	
	private static StringWithVars build(String val) {
		checkNotNull(val);
	    String original = val;
	    
		ImmutableList.Builder<Variable> entriesBuilder = ImmutableList.builder();
		StringBuilder sb = new StringBuilder();
		
		Matcher matcher = pattern.matcher(val);
		
		int pos = 0;
		
		while (matcher.find()) {
			if (matcher.start() > pos) {
				sb.append(val, pos, matcher.start());
			}
			String path = matcher.group(1).trim();
			entriesBuilder.add(new ColonVariable(dots(path), sb.length()));
			pos = matcher.end();
		}
		
		if (pos < val.length()) {
			sb.append(val, pos, val.length());
		}
		
		ImmutableList<Variable> entries = entriesBuilder.build();
		
		if (entries.size() > 0) {
			return new StringWithColonVariables(original, sb.toString(), entries);
		} else {
			return new StringWithoutVariables(original);
		}
	}
	
	private StringWithColonVariables(String original, String base, List<Variable> entries) {
		this.original = original;
		this.base = base;
		this.entries = entries;
	}

	@Override
	public String apply(Data data) {
		StringBuilder output = new StringBuilder(base);
		int offset = 0;
		for (Variable _var : entries) {
			
			ColonVariable var = (ColonVariable) _var;
			
			Data d = data.at(var.path);
			
			if (d.isPresent()) {
				String val;
				if (d.isContent()) {
					val = d.content().asUTF8();
				} else {
					val = d.toPrettyJson();
				}
				if (val != null) {
					output.insert(var.pos + offset, val);
					offset += val.length();
				}
			}
		}
		return output.toString();
	}
	
	@Override
	public String withPlaceholder(String val) {
		StringBuilder output = new StringBuilder(base);
		int offset = 0;
		for (Variable _var : entries) {
			ColonVariable var = (ColonVariable) _var;
			output.insert(var.pos + offset, val);
			offset += val.length();
		}
		return output.toString();
	}

	@Override
	public List<Variable> vars() {
		return entries;
	}

	@Override
	public boolean hasVariables() {
		return true;
	}
	
	@Override
	public String toString() {
	    return original;
	}
	
	
}
