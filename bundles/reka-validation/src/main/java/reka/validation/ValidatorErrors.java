package reka.validation;

import reka.api.Path;

public interface ValidatorErrors {
	void add(Path path, String msg);
}