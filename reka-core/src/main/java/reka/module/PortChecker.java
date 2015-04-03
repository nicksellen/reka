package reka.module;

import java.util.Optional;

import reka.identity.Identity;

public interface PortChecker {
	boolean check(Identity identity, int port, Optional<String> host);
}
