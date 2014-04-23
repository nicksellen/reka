package reka.test.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.api.Path.path;
import static reka.api.content.Contents.nonSerializableContent;

import org.junit.Test;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

public class NonSerializableObjectDatasetTest {
    
    class NonSerializableObject {
        String name;
    }
    
    @Test
    public void test() {
        NonSerializableObject obj = new NonSerializableObject();
        obj.name = "example";
        MutableData data = MutableMemoryData.create();
        Path path = path("somewhere", "in", "here");
        
        data.put(path, nonSerializableContent(obj));
        NonSerializableObject val = data.getContent(path).get().valueAs(NonSerializableObject.class);
        assertThat(val.name, equalTo("example"));
        
    }

}
