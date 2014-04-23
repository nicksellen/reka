package nicksellen.flow.util;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.api.Path.path;
import static reka.api.content.Contents.utf8;

import org.junit.Test;
import org.mozilla.javascript.Script;

import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;
import reka.javascript.JavascriptRhinoHelper;

public class JSHelperTest {

	@Test
	public void basicTest() {
		
		String subject = "my lovely subject";
		String from = "Peter <peter@gmail.com>";
		
		MutableData data = MutableMemoryData.create();
		data.put(path("subject"), utf8(subject));
		data.put(path("from"), utf8(from));
		
		Script script = JavascriptRhinoHelper.compileJavascript("exports = subject + ' from ' + from");
		String result = JavascriptRhinoHelper.runJavascript(script, data).toString();
		
		assertThat(result, equalTo(format("%s from %s", subject, from)));
		
	}
}
