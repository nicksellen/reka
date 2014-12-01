package reka;

import java.util.Optional;

public interface PortChecker {
	boolean check(String identity, int port, Optional<String> host);
}
