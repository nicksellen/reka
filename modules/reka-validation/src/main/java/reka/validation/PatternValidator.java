package reka.validation;

import static java.lang.String.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.Path;
import reka.api.data.Data;

public class PatternValidator implements ValidatorRule {
	
	private final Path path;
	private final Pattern pattern;
	
	public PatternValidator(Path path, Pattern pattern) {
		this.path = path;
		this.pattern = pattern;
	}

	@Override
	public void validate(Data data, ValidatorErrors errors) {
		data.getContent(path).ifPresent(content -> {
			String val = content.asUTF8();
			Matcher matcher = pattern.matcher(val);
			if (!matcher.find()) {
				errors.add(path, format("does not match pattern %s", pattern.pattern()));
			}
		});
	}

}
