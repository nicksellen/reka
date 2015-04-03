package reka.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static reka.util.Path.root;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import reka.data.DiffContentConsumer.DiffContentType;
import reka.data.DiffPathConsumer.DiffPathType;
import reka.data.content.Content;
import reka.util.Path;
import reka.util.Path.PathElement;

final class DataUtils {
	
	private static final JsonFactory jsonFactory = new JsonFactory();

	static void visitFirstContent(Data data, BiConsumer<Path,Content> visitor) {
		visitFirstContent(data, visitor, root());
	}
	
	private static void visitFirstContent(Data data, BiConsumer<Path,Content> visitor, Path current) {
		if (data.isContent()) {
			visitor.accept(current, data.content());
			return;
		} else {
			for (Entry<PathElement, Data> entry : data) {
				visitContent(entry.getValue(), visitor, current.add(entry.getKey()));
			}
		}
	}
	
	static Content getFirstContent(Data data) {
		if (data.isContent()) {
			return data.content();
		} else {
			for (Entry<PathElement, Data> entry : data) {
				Content c = getFirstContent(entry.getValue());
				if (c != null) return c;
			}
		}
		return null;
	}
	
	static void visitEachContent(Data data, BiConsumer<Path,Content> visitor) {
		visitContent(data, visitor, root());
	}
	
	private static void visitContent(Data data, BiConsumer<Path,Content> visitor, Path current) {
		if (data.isContent()) {
			visitor.accept(current, data.getContent(root()).get());
		} else {
			for (Entry<PathElement, Data> entry : data) {
				visitContent(entry.getValue(), visitor, current.add(entry.getKey()));
			}
		}
	}
	
	static Data getDataAtPath(Data result, Path path) {
		for (PathElement e : path) {
			checkArgument(!e.isNextIndex(), "cannot use next-index element during a get...!");
			result = result.at(e);
		}
		return result;
	}
	
	public static String writeDataToPrettyJson(Data data) {
		try {
			StringWriter writer = new StringWriter();
			JsonGenerator json = jsonFactory.createJsonGenerator(writer);
			json.useDefaultPrettyPrinter();
			data.writeJsonTo(json);
			json.flush();
			return writer.toString();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public static String writeDataToJson(Data data) {
		try {
			StringWriter writer = new StringWriter();
			JsonGenerator json = jsonFactory.createJsonGenerator(writer);
			data.writeJsonTo(json);
			json.flush();
			return writer.toString();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public static boolean dataIsEqual(Data a, Data b) {
		if (a == b) return true;
		
		if (a.isContent() && b.isContent()) {
			return a.content().equals(b.content());
		} else if (a.isContent() || b.isContent()) {
			return false;
		}
		
		Collection<PathElement> aElements = a.elements();
		Collection<PathElement> bElements = b.elements();
		
		if (aElements.size() != bElements.size() || !aElements.equals(bElements)) {
			return false;
		}
		
		for (PathElement e : aElements) {
			if (!dataIsEqual(a.at(e), b.at(e))) {
				return false;
			}
		}
		
		return true;
	}

	static void diffContent(Data a, Data b, DiffContentConsumer visitor) {
		diffContentWithBasePath(root(), a, b, visitor);
	}
	
	static void diffPath(Data a, Data b, DiffPathConsumer visitor) {
		diffPathsWithBasePath(root(), a, b, visitor);
	}

	private static void diffPathsWithBasePath(Path base, Data a, Data b, DiffPathConsumer visitor) {
		
		if (a.isContent() && b.isContent()) {
			Content aContent = a.getContent(root()).get();
			Content bContent = b.getContent(root()).get();
			if (!aContent.equals(bContent)) {
				visitor.accept(base, DiffPathType.CHANGED);
			}
		} else if (a.isContent()) {
			visitor.accept(base, DiffPathType.REMOVED);
		} else if (b.isContent()) {
			visitor.accept(base, DiffPathType.ADDED);
		}
		
		Set<PathElement> aElements = new HashSet<>(a.elements());
		Set<PathElement> bElements = new HashSet<>(b.elements());
		
		if (aElements.isEmpty() && bElements.isEmpty()) return;
		
		// appears in both
		
		intersection(aElements, bElements).forEach(e -> {
			diffPathsWithBasePath(base.add(e), a.at(e), b.at(e), visitor);	
		});
		
		// removed
		
		difference(aElements, bElements).forEach(e -> {
			visitor.accept(base.add(e), DiffPathType.REMOVED);
		});

		// added
		
		difference(bElements, aElements).forEach(e -> {
			visitor.accept(base.add(e), DiffPathType.ADDED);
		});
		
	}
	
	private static void diffContentWithBasePath(Path base, Data a, Data b, DiffContentConsumer visitor) {
		
		if (a.isContent() && b.isContent()) {
			Content aContent = a.getContent(root()).get();
			Content bContent = b.getContent(root()).get();
			if (!aContent.equals(bContent)) {
				visitor.accept(base, DiffContentType.CHANGED, aContent, bContent);
			}
		} else if (a.isContent()) {
			visitor.accept(base, DiffContentType.REMOVED, a.getContent(root()).get(), null);
		} else if (b.isContent()) {
			visitor.accept(base, DiffContentType.ADDED, null, b.getContent(root()).get());
		}
		
		Set<PathElement> aElements = new HashSet<>(a.elements());
		Set<PathElement> bElements = new HashSet<>(b.elements());
		
		if (aElements.isEmpty() && bElements.isEmpty()) return;
		
		// appears in both
		
		intersection(aElements, bElements).forEach(e -> {
			diffContentWithBasePath(base.add(e), a.at(e), b.at(e), visitor);	
		});
		
		// removed
		
		difference(aElements, bElements).forEach(e -> {
			Path eBase = base.add(e);
			a.at(e).forEachContent((path, content) -> {
				visitor.accept(eBase.add(path), DiffContentType.REMOVED, content, null);
			});
		});

		// added
		
		difference(bElements, aElements).forEach(e -> {
			Path eBase = base.add(e);
			b.at(e).forEachContent((path, content) -> {
				visitor.accept(eBase.add(path), DiffContentType.ADDED, null, content);
			});
		});
		
	}
	
}
