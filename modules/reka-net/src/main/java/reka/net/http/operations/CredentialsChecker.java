package reka.net.http.operations;

public interface CredentialsChecker {
	boolean check(String username, String password);
}
