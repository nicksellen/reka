package reka.http.operations;

import java.util.Map;

public class MapCredentialsChecker implements CredentialsChecker {

	private final Map<String,String> credentials;
	
	public MapCredentialsChecker(Map<String,String> credentials) {
		this.credentials = credentials;
	}
	
	@Override
	public boolean check(String username, String password) {
		String pass = credentials.get(username);
		if (pass != null && pass.equals(password)) {
			return true;
		}
		return false;
	}

}
