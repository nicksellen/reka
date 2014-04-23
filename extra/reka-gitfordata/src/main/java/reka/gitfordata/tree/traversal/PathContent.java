package reka.gitfordata.tree.traversal;

import java.util.Map.Entry;

import reka.api.Path;
import reka.api.content.Content;

public interface PathContent extends Entry<Path,Content> {
	Path path();
	Content content();
}