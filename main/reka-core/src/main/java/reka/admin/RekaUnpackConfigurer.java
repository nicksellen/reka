package reka.admin;

import static reka.api.Path.dots;

import java.util.UUID;
import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.dirs.AppDirs;

public class RekaUnpackConfigurer implements OperationConfigurer {

	private final AppDirs dirs;
	private Function<Data, Path> dataPathFn;

	private Function<Data, String> identityFn = (unused) -> UUID.randomUUID().toString();

	RekaUnpackConfigurer(AppDirs dirs) {
		this.dirs = dirs;
	}

	@Conf.At("data")
	public void filename(String val) {
		dataPathFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}

	@Conf.At("identity")
	public void identity(String val) {
		identityFn = StringWithVars.compile(val);
	}

	@Override
	public void setup(OperationSetup ops) {
		//ops.add("unpack", store -> new RekaUnpackOperation(dirs, dataPathFn, identityFn));
	}

}