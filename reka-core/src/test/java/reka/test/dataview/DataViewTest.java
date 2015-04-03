package reka.test.dataview;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;

public class DataViewTest {
	
	private final ObjectMapper mapper = new ObjectMapper();
	
	@Test
	public void test() throws JsonGenerationException, JsonMappingException, IOException {
		MutableData data = MutableMemoryData.create();
		
		data.putMap("yay", m -> {
			m.putString("name", "nick");
			m.putInt("age", 30);
			m.putList("interests", l -> {
				l.addString("swimming");
				l.addString("cycling");
				l.addString("beer");
			});
		});
		
		String json1 = data.toJson();
		
		System.out.printf("json1: %s\n", json1);
		System.out.printf("json2: %s\n", mapper.writeValueAsString(data.viewAsMap()));
		System.out.printf("json3: %s\n", mapper.writeValueAsString(data.toMap()));
		
	}

}
