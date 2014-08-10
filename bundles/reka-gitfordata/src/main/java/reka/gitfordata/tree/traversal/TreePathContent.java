package reka.gitfordata.tree.traversal;

import static reka.util.Util.unsupported;
import reka.api.Path;
import reka.api.content.Content;
import reka.gitfordata.tree.record.ContentRecord;

public class TreePathContent implements PathContent {

	private final Path path;
	private final ContentRecord content;
	
	public TreePathContent(Path path, ContentRecord content) {
		this.path = path;
		this.content = content;
	}
	
	@Override
	public Path path() {
		return path;
	}
	
	@Override
	public Content content() {
		return content != null ? content.toContent() : null;
	}

	@Override
	public Path getKey() {
		return path();
	}

	@Override
	public Content getValue() {
		return content();
	}

	@Override
	public Content setValue(Content value) {
		throw unsupported();
	}

}
