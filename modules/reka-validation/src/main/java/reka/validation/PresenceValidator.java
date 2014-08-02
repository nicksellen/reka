package reka.validation;

import reka.api.Path;
import reka.api.data.Data;

public class PresenceValidator implements ValidatorRule {

	private final Path path;
	
	public PresenceValidator(Path path) {
		this.path = path;
	}
	
	@Override
	public void validate(Data data, ValidatorErrors errors) {
		
		Data v = data.at(path);
		
		if (!v.isPresent() || (v.isContent() && v.content().isEmpty())) {
			errors.add(path, "required");
		}
		
	}
	
}