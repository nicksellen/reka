package reka.runtime;

import reka.runtime.handlers.ErrorHandler;
import reka.runtime.handlers.HaltedHandler;

public interface FailureHandler extends HaltedHandler, ErrorHandler {
}