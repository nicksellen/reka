package reka.config.processor;

import reka.config.Config;

public class CommentConverter implements ConfigConverter {

	@Override
	public void convert(Config config, Output out) {
		if (config.key().startsWith("//")) {
			// ignore it
		} else {
			out.add(config); // passthrough
		}
	}

}
