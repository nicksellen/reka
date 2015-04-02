package reka.core.builder;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.api.Path.PathElements.nextIndex;

import java.util.List;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.builder.FlowVisualizer.GraphVisualizer;
import reka.core.builder.FlowVisualizer.NodeType;
import reka.core.data.memory.MutableMemoryData;

public class JsonGraphVisualizer implements GraphVisualizer<String> {
	
	private final MutableData data;
	
	public JsonGraphVisualizer() {
		data = MutableMemoryData.create();
	}
	
	@Override
	public void node(int id, String name, NodeType type) {
		data.putMap(path(name("nodes"), nextIndex()), map -> {
			map.putInt("id", id);
			map.putString("name", name);
			map.putString("type", type.toString());
		});
	}
	
	@Override
	public void group(Path path, int id) {
		data.putInt(path("groups").add(path).add("@nodes").add(nextIndex()), id);
	}
	
	@Override
	public void connect(int from, int to, String label, boolean optional) {
		data.putMap(path(name("connections"), nextIndex()), map -> {
			map.putInt("from", from);
			map.putInt("to", to);
			map.putBool("optional", optional);
			if (label != null) {
				map.putString("label", label);
			}
		});
	}

	@Override
	public void meta(int id, List<Data> metas) {
		data.putMap(path(name("meta"), nextIndex()), map -> {
			map.putInt("id", id);
			map.putList("metas", list -> {
				metas.forEach(meta -> {
					list.add(meta);
				});
			});
		});
	}
	
	@Override
	public String build() {
		return data.toJson();
	}
	
}