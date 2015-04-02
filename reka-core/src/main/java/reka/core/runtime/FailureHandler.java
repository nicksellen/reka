package reka.core.runtime;

import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.HaltedHandler;

public interface FailureHandler extends HaltedHandler, ErrorHandler {
}