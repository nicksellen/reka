package reka.common.markdown;

import static reka.api.content.Contents.utf8;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class MarkdownOperation implements Operation {

	private final Function<Data,Path> inFn, outFn;
	
	public MarkdownOperation(Function<Data,Path> inFn, Function<Data,Path> outFn) {
		this.inFn = inFn;
		this.outFn = outFn;
	}
	
	@Override
	public void call(MutableData data) {
		data.at(inFn.apply(data)).forEachContent((path, content) -> {
			content = utf8(MarkdownBundle.md.get().markdownToHtml(content.asUTF8()));
			data.put(outFn.apply(data).add(path), content);
		});
	}
	
}
