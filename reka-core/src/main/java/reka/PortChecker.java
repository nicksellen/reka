package reka;

import java.util.Optional;

public interface PortChecker {
	boolean check(Identity identity, int port, Optional<String> host);
}
