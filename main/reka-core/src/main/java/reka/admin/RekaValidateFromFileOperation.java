package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.codehaus.jackson.map.ObjectMapper;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;
import reka.config.FileSource;
import reka.configurer.Configurer.InvalidConfigurationException;
import reka.core.data.memory.MutableMemoryData;
import reka.util.Util;

public class RekaValidateFromFileOperation implements RoutingOperation {
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	private final ApplicationManager manager;
	private final Function<Data,String> filenameFn;
	
	public RekaValidateFromFileOperation(ApplicationManager manager, Function<Data,String> filenameFn) {
		this.manager = manager;
		this.filenameFn = filenameFn;
	}

	@Override
	public MutableData call(MutableData data, RouteCollector router) {
		try {
			String filename = filenameFn.apply(data);
			File file = new File(filename);
			checkArgument(file.exists(), "file does not exist [%s]", filename);
			checkArgument(!file.isDirectory(), "path is a directory [%s]", filename);
			manager.validate(FileSource.from(file));
			router.routeTo("ok");
			return data;
		} catch (Throwable t) {
			t = Util.unwrap(t);
			if (t instanceof InvalidConfigurationException) {
				InvalidConfigurationException e = (InvalidConfigurationException) t;
				try {
					
					Map<String,Object> map = new HashMap<>();
					map.put("errors", jsonMapper.readValue(e.toJson(), List.class));
					
					MutableMemoryData.createFromMap(map).forEachContent((p, c) -> {
						data.put(p, c);
					});
					
				} catch (IOException e1) {
					data.putString("error", e.getMessage());
				}
			} else {
				data.putString("error", t.getMessage());
			}
			router.routeTo("error");
		}
		return data;
	}
	
}