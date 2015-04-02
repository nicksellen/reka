package reka.crypto.bcrypt;

import org.mindrot.jbcrypt.BCrypt;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;

public class BCryptCheckpwOperation implements RouterOperation {

	static final RouteKey OK = RouteKey.named("ok");
	static final RouteKey FAIL = RouteKey.named("fail");

	private final Path readPwFrom;
	private final Path readHashFrom;

	public BCryptCheckpwOperation(Path readPwFrom, Path readHashFrom) {
		this.readPwFrom = readPwFrom;
		this.readHashFrom = readHashFrom;
	}
	
	@Override
	public void call(MutableData data, RouteCollector router) {
		router.defaultRoute(FAIL);
		data.getContent(readPwFrom).ifPresent(pw -> {
			data.getContent(readHashFrom).ifPresent(hash -> {
				if (BCrypt.checkpw(pw.asUTF8(), hash.asUTF8())) {
					router.routeTo(OK);
				}
			});
		});
	}
	
}