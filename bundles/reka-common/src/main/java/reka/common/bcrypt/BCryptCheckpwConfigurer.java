package reka.common.bcrypt;

import static reka.api.Path.dots;
import static reka.config.configurer.Configurer.configure;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationSetup;
import reka.nashorn.OperationConfigurer;

public class BCryptCheckpwConfigurer implements OperationConfigurer {
	
	private final ConfigurerProvider provider;
	
	private Path readPwFrom = dots("bcrypt.pw");
	private Path readHashFrom = dots("bcrypt.hash");
	
	private OperationConfigurer ok;
	private OperationConfigurer fail;
	
	public BCryptCheckpwConfigurer(ConfigurerProvider provider) {
		this.provider = provider;
	}

	@Conf.At("read-pw-from")
	public void readPwFrom(String val) {
		readPwFrom = dots(val);
	}
	
	@Conf.At("read-hash-from")
	public void readHashFrom(String val) {
		readHashFrom = dots(val);
	}
	
	@Conf.At("ok")
	public void ok(Config config) {
		ok = configure(new SequenceConfigurer(provider), config);
	}
	
	@Conf.At("fail")
	public void fail(Config config) {
		fail = configure(new SequenceConfigurer(provider), config);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.router("bcrypt/checkpw", store -> new BCryptCheckpwOperation(readPwFrom, readHashFrom), router -> {
			router.add(BCryptCheckpwOperation.OK, ok);
			router.add(BCryptCheckpwOperation.FAIL, fail);
		});
	}
	
}