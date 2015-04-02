package reka.config.configurer;

import reka.config.configurer.Configurer.ErrorCollector;

public interface ErrorReporter {
	void errors(ErrorCollector errors);
}
