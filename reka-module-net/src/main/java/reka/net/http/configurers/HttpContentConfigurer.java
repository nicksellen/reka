package reka.net.http.configurers;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.data.content.Contents.binary;
import static reka.data.content.Contents.utf8;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.data.content.Content;
import reka.data.content.types.BinaryContent;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.net.http.operations.HttpContentUtils;
import reka.util.dirs.AppDirs;

public class HttpContentConfigurer implements OperationConfigurer {
	
	private final AppDirs dirs;
	private Content content;
	private String contentType;
	
	public HttpContentConfigurer(AppDirs dirs) {
		this.dirs = dirs;
	}
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			checkConfig(config.documentType() != null, "no document type!");
			content = binary(config.documentType(), config.documentContent());
			if (config.documentType() != null) {
				contentType = config.documentType();
			}
		} else if (config.hasValue()) {
			content = utf8(config.valueAsString());
			contentType = "text/plain";
		}
	}
	
	@Conf.At("content")
	public HttpContentConfigurer content(String value) {
		content = utf8(value);
		return this;
	}

	public HttpContentConfigurer content(Data value) {
		content = utf8(value.toJson());
		contentType = "application/json";
		return this;
	}

	@Conf.At("content-type")
	public HttpContentConfigurer contentType(String value) {
		contentType = value;
		return this;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		if (content instanceof BinaryContent && contentType == null) {
			String ct = ((BinaryContent) content).contentType();
			if (ct != null) {
				contentType = ct;
			}
		}
		ops.add("content", () -> HttpContentUtils.httpContent(dirs.tmp(), content, contentType, true));
	}

}
