package reka.flow.builder;

import static reka.util.Path.path;
import static reka.util.Path.PathElements.name;
import static reka.util.Path.PathElements.nextIndex;

import java.util.List;

import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.builder.FlowVisualizer.GraphVisualizer;
import reka.flow.builder.FlowVisualizer.NodeType;
import reka.util.Path;

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