package reka.validation;

import reka.api.data.Data;

public interface ValidatorRule {
	void validate(Data data, ValidatorErrors errors);
}