package reka.configurer;

import reka.configurer.Configurer.ErrorCollector;

public interface ErrorReporter {
	void errors(ErrorCollector errors);
}
