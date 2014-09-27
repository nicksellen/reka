package reka.http.operations;

import static java.lang.String.format;
import static reka.util.Util.unchecked;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;

import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;

public class BasicAuthRouter implements RouterOperation {
	
	public static final RouteKey OK = RouteKey.named("ok");
	public static final RouteKey FAIL = RouteKey.named("fail");
	
	private static final String PREFIX = "Basic";
	private final String unauthorized;

	private final CredentialsChecker checker;
	
	public BasicAuthRouter(String realm, CredentialsChecker checker) {
		this.unauthorized = format("Basic realm=\"%s\"", realm);
		this.checker = checker;
	}

	@Override
	public void call(MutableData data, RouteCollector router) {
		if (checkBasicAuth(data)) {
			router.routeTo(OK);
		} else {
			data.putInt(Response.STATUS, 401)
				.putString(Response.HEADERS.add("WWW-Authenticate"), unauthorized);
			router.routeTo(FAIL);
		}
	}
	
	private boolean checkBasicAuth(MutableData data) {
		String authorization = data.getString(Request.HEADERS.add("Authorization")).orElse(null);
        try {
            if (authorization != null) {
                final int space = authorization.indexOf(' ');
                if (space > 0) {
                    final String method = authorization.substring(0, space);
                    if (PREFIX.equalsIgnoreCase(method)) {
                    	Decoder b64 = Base64.getDecoder();
                        final String decoded = new String(b64.decode(authorization.substring(space + 1)), StandardCharsets.UTF_8);
                        final int i = decoded.indexOf(':');
                        if (i > 0) {
                            String username = decoded.substring(0, i);
                            String password = decoded.substring(i + 1);
                            return checker.check(username, password);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
        	throw unchecked(e);
        }
        return false;
	}

}
