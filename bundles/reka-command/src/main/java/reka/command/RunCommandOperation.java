package reka.command;

import static java.util.Arrays.asList;
import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

public class RunCommandOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String[] cmd;
	
	public RunCommandOperation(String exec, List<String> args) {
		List<String> parts = new ArrayList<>();
		for (String s : Splitter.on(" ").split(exec)) {
			parts.add(s);
		}
		parts.addAll(args);
		cmd = parts.toArray(new String[parts.size()]);
	}
	
	@Override
	public MutableData call(MutableData data) {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command(cmd);
			log.debug("running {}{}\n", "", asList(cmd), "");
			Process process = builder.start();
			
			InputStream err = process.getErrorStream();
			InputStream out = process.getInputStream();
			
			ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			
			while (process.isAlive() || err.available() > 0 || out.available() > 0) {
				if (err.available() > 0) {
					errBytes.write(err.read());
				}
				if (out.available() > 0) {
					outBytes.write(out.read());
				}
			}
			
			process.waitFor(5, TimeUnit.SECONDS);
			
			data.putString("stderr", new String(errBytes.toByteArray(), Charsets.UTF_8));
			data.putString("stdout", new String(outBytes.toByteArray(), Charsets.UTF_8));
			data.putInt("exit", process.exitValue());
			
			return data;
		} catch (IOException | InterruptedException e) {
			throw unchecked(e);
		}
	}

}
