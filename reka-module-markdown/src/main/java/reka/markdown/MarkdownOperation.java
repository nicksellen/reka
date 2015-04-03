package reka.markdown;

import static reka.data.content.Contents.utf8;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;

public class MarkdownOperation implements Operation {

	private final Function<Data,Path> inFn, outFn;
	
	public MarkdownOperation(Function<Data,Path> inFn, Function<Data,Path> outFn) {
		this.inFn = inFn;
		this.outFn = outFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.at(inFn.apply(data)).forEachContent((path, content) -> {
			content = utf8(MarkdownModuleConfigurer.md.get().markdownToHtml(content.asUTF8()));
			data.put(outFn.apply(data).add(path), content);
		});
	}
	
}
