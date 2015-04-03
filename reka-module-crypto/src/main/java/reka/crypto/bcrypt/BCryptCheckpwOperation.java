package reka.crypto.bcrypt;

import org.mindrot.jbcrypt.BCrypt;

import reka.api.Path;
import reka.data.MutableData;
import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;

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