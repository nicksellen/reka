package reka.exec;

import static reka.api.Path.root;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class ExecConfigurer extends ModuleConfigurer {
	
	private String[] command;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			setScript(config.documentContent());
		}
	}
	
	private void setScript(byte[] script) {
		try {
			File file = Files.createTempFile(dirs().tmp(), "script.", "").toFile();
			Files.write(file.toPath(), script);
			file.deleteOnExit();
			file.setExecutable(true, true);
			command = new String[] { file.getAbsolutePath() };
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	/*
	
	
	@Conf.EachChildOf("args")
	public void arg(Config config) {
		args.add(config.key());
		if (config.hasValue()) {
			args.add(config.valueAsString());
		}
	}
	
	@Conf.Each("arg")
	public void arg(String val) {
		args.add(val);
	}
	*/
	
	@Override
	public void setup(ModuleSetup module) {
		module.operation(root(), provider -> new ExecRunConfigurer(command));
	}

}
