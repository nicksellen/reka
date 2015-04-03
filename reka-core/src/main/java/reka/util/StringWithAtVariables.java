package reka.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static reka.api.Path.dots;
import static reka.data.content.Contents.utf8;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.Path;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;

import com.google.common.collect.ImmutableList;

final class StringWithAtVariables implements Function<Data,String>, StringWithVars {
	
	private static final Pattern pattern = Pattern.compile("@\\{([a-zA-Z0-9-_\\.\\[\\]]+\\b)\\}");
	
	public static void main(String[] args) throws Exception {
		
		Function<Data,String> v = StringWithVars
				.compile("this :special.thing should match :person.name [:person.name] :{person.name}oh yay with no match [:nothing.to.match] :)");
		
		MutableData data = MutableMemoryData.create();
		data.put(dots("special.thing"), utf8("heya!"));
		data.put(dots("person.name"), utf8("a name!"));
		
		String out = v.apply(data);
		System.out.printf("formatted: %s\n", out);
		
		System.out.printf("no variables [%s]\n", StringWithVars.compile("a nice normal string").apply(data));
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
	
	public static boolean hasVars(String input) {
		return pattern.matcher(input).find();
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
			return new StringWithAtVariables(original, sb.toString(), entries);
		} else {
			return new StringWithoutVariables(original);
		}
	}
	
	private StringWithAtVariables(String original, String base, List<Variable> entries) {
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
					val = d.toJson();
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
		for (Variable v : entries) {
			ColonVariable var = (ColonVariable) v;
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

	@Override
	public String original() {
		return original;
	}
	
	
}
