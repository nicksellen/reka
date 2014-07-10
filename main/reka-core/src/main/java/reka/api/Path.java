package reka.api;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.util.Util.unchecked;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.hash.Hasher;
import com.google.common.io.BaseEncoding;

public class Path implements Iterable<Path.PathElement>, Comparable<Path>, Hashable {

	private static final BaseEncoding HEX = BaseEncoding.base16();
	private static final Pattern INDEX_MATCHER = Pattern.compile("^([^\\[\\]]*)\\[([0-9]+|\\+)?\\]$");

	private static final Pattern DOT_SCANNER = Pattern.compile("(?:[^\\\\])\\.+");

	private static final Splitter slashSplitter = Splitter.on("/").omitEmptyStrings();
	private static final Joiner slashJoiner = Joiner.on("/").skipNulls();
	
	private final PathElement[] elements;
	private final int size;
	
	public static Path fromURL(String url) {
		Path.Builder builder = newBuilder();
		try {
			for (PathElement e : slashes(url)) {
				if (e.isKey()) {
					builder.add(URLDecoder.decode(e.name(), Charsets.UTF_8.name()));
				} else {
					builder.add(e);
				}
			}
		} catch (UnsupportedEncodingException e) { throw unchecked(e); }
		return builder.build();
	}
	
	public Path reverse() {
		PathElement[] newElements = new PathElement[elements.length];
		System.arraycopy(elements, 0, newElements, 0, elements.length);
		ArrayUtils.reverse(newElements);
		return new Path(newElements);
	}
	
	public boolean containsNextIndex() {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].isNextIndex()) return true;
		}
		return false;
	}
	
	public Path parent() {
		return size > 0 ? subpath(0, size - 1) : null;
	}
	
	public Path[] parents() {
		if (size < 2) return new Path[0];
		Path[] parents = new Path[size];
		Path parent = parent();
		int i = 0;
		while (parent != null) {
			parents[i++] = parent;
			parent = parent.parent();
		}
		return parents;
	}

	public String toURL() {
		StringBuilder s = new StringBuilder();
		try {
			for (PathElement e : this) {
				if (e.isKey()) {
					s.append(URLEncoder.encode(e.name(), Charsets.UTF_8.name()).replaceAll("\\+", "%20"));
				} else {
					s.append(e.toString());
				}
				if (!e.equals(last())) s.append("/");
			}
		} catch (UnsupportedEncodingException e) { throw unchecked(e); }
		return s.toString();
	}
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class PathElements {
		public static PathElement name(String name) {
			return new NamedPathElement(name);
		}
		public static PathElement index(int index) {
			return new IndexedPathElement(index);
		}
		public static PathElement nextIndex() {
			return new NextIndexPathElement();
		}
		public static PathElement empty() {
			return EmptyPathElement.INSTANCE;
		}
	}
	
	public static abstract class PathElement implements Comparable<PathElement>, Hashable {
		public boolean isKey() { return false; }
		public boolean isIndex() { return false; }
		public boolean isNextIndex() { return false; }
		public boolean isEmpty() { return false; }
		public int index() { return -1; }
		public String name() { return null; }
	}
	
	private static class EmptyPathElement extends PathElement {
		
		static final EmptyPathElement INSTANCE = new EmptyPathElement();
		
		private EmptyPathElement() { }
		
		@Override
		public boolean isEmpty() { return true; }

		@Override
		public int compareTo(PathElement o) {
			return 0;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher; // is nothing ok?
		}
		
		@Override
		public String toString() {
			return "";
		}
	}
	
	private static class IndexedPathElement extends PathElement {
		
		private final int index;
		
		IndexedPathElement(int index) {
			this.index = index;
		}
		
		@Override
		public boolean isIndex() { return true; }
		
		@Override
		public int index() { return index; }

		@Override
		public int compareTo(PathElement o) {
			if (o instanceof IndexedPathElement) {
				return Integer.compare(index, ((IndexedPathElement)o).index());
			} else if (o.isKey()) {
				return 1;
			} else {
				return -1;
			}
		}
		
		@Override
		public String toString() {
			return format("[%d]", index);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(index);
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof IndexedPathElement)) return false;
			return index == ((IndexedPathElement)object).index;
		}
		
		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putInt(index);
		}
	}
	
	private static class NextIndexPathElement extends PathElement {
		
		@Override
		public boolean isNextIndex() { return true; }

		@Override
		public int compareTo(PathElement o) {
			if (o instanceof NextIndexPathElement) {
				return 0;
			} else if (o.isKey()) {
				return 1;
			} else {
				return -1;
			}
		}
		
		@Override
		public String toString() {
			return "[]";
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(93874938749837498L);
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof NextIndexPathElement;
		}
		
		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putInt(-5765765);
		}
	}
	
	private static class NamedPathElement extends PathElement {
		
		private final String name;
		
		private NamedPathElement(String name) {
			assert name != null;
			this.name = name;
		}
		
		@Override
		public boolean isKey() { return true; }
		
		@Override
		public String name() { return name; 
		
		}
		@Override
		public String toString() {
			return name;
		}
		
		@Override
		public int compareTo(PathElement o) {
			if (o instanceof NamedPathElement) {
				return name.compareTo(((NamedPathElement)o).name);
			} else if (o.isIndex()) {
				return -1;
			} else {
				return 1;
			}
		}
		
		@Override
		public boolean equals(Object object) {
			if (!(object instanceof NamedPathElement)) return false;
			NamedPathElement other = (NamedPathElement) object;
			return name.equals(other.name);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(name);
		}
		
		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putString(name);
		}
		
	}
	
	public static class Builder {
		private final List<PathElement> elements = new ArrayList<>();
		public Builder add(String element) {
			elements.add(PathElements.name(element));
			return this;
		}
		public Builder add(int index) {
			elements.add(PathElements.index(index));
			return this;
		}
		public Builder add(PathElement element) {
			if (!element.isEmpty()) {
				elements.add(element);
			}
			return this;
		}
		public Builder add(Iterator<PathElement> iterator) {
			iteratorToList(elements, iterator);
			return this;
		}
		public Builder remove(int i) {
			elements.remove(i);
			return this;
		}
		public Path build() {
			return new Path(elements.toArray(new PathElement[elements.size()]));
		}
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(toArray());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;		
		if (!(obj instanceof Path)) return false;
		
		Path other = (Path) obj;
		
		if (other.size != size) return false;
		
		for (int i = 0; i < size; i++) {
			if (!elements[i].equals(other.elements[i])) {
				return false;
			}
		}
		
		return true;
	}

	private static final Path EMPTY_PATH = new Path();
	
	public static final Path REQUEST 	= Path.path("request");
	public static final Path RESPONSE 	= Path.path("response");
	
	public static final class Request {
		public static final Path METHOD 	= REQUEST.add("method");
		public static final Path PATH 		= REQUEST.add("path");
		public static final Path PARAMS 	= REQUEST.add("params");
		public static final Path DATA 		= REQUEST.add("data");
		public static final Path UPLOADS 	= REQUEST.add("uploads");
		public static final Path CONTENT 	= REQUEST.add("content");
		public static final Path HEADERS 	= REQUEST.add("headers");
		public static final Path COOKIES 	= REQUEST.add("cookies");
		public static final Path HOST 		= REQUEST.add("host");
		public static final class Headers {
			public static final Path CONTENT_TYPE 	= HEADERS.add("Content-Type");
			public static final Path CONTENT_LENGTH = HEADERS.add("Content-Length");
			public static final Path IF_NONE_MATCH 	= HEADERS.add("If-None-Match");
			public static final Path AUTHORIZATION 	= HEADERS.add("Authorization");
			public static final Path ACCEPT 		= HEADERS.add("Accept");
		}
	}
	
	public static final class Response {
		public static final Path STATUS 	= RESPONSE.add("status");
		public static final Path CONTENT 	= RESPONSE.add("content");
		public static final Path HEADERS 	= RESPONSE.add("headers");
		public static final Path COOKIES	= RESPONSE.add("cookies");
		public static final Path HEAD 		= RESPONSE.add("head"); // are we sending a head response
		public static final class Headers {
			public static final Path DATE 				 = HEADERS.add("Date");
			public static final Path LOCATION 			 = HEADERS.add("Location");
			public static final Path CONTENT_TYPE 		 = HEADERS.add("Content-Type");
			public static final Path CONTENT_DISPOSITION = HEADERS.add("Content-Disposition");
			public static final Path CONTENT_LENGTH 	 = HEADERS.add("Content-Length");
			public static final Path ETAG 				 = HEADERS.add("ETag");
			public static final Path LINK 				 = HEADERS.add("Link");
			public static final Path EXPIRES 			 = HEADERS.add("Expires");
			public static final Path CACHE_CONTROL 		 = HEADERS.add("Cache-Control");
			public static final Path WWW_AUTHENTICATE 	 = HEADERS.add("WWW-Authenticate");
		}
	}
	
	public static Path slashes(String value) {
		return fromStringWithSplitter(value, slashSplitter);
	}
	
	public static Path dots(String value) {
		
		List<PathElement> elements = new ArrayList<>();
		Matcher m = DOT_SCANNER.matcher(value);
		int pos = 0;
		while (m.find()) {
			String segment = value.substring(pos, m.start() + 1);
			addElementTo(segment.replaceAll("\\\\", ""), elements);
			pos = m.end();
		}
		
		if (pos < value.length()) {
			addElementTo(value.substring(pos, value.length()).replaceAll("\\\\", ""), elements);
		}
		
		return new Path(elements.toArray(new PathElement[elements.size()]));
	}
	
	private static void addElementTo(String element, Collection<PathElement> elements) {
		Matcher matcher = INDEX_MATCHER.matcher(element);
		if (matcher.matches()) {
			String firstbit = matcher.group(1);
			if (firstbit != null && !"".equals(firstbit)) {
				elements.add(PathElements.name(firstbit));
			}
			String indexStr = matcher.group(2);
			if ("+".equals(indexStr) || indexStr == null) {
				elements.add(PathElements.nextIndex());
			} else {
				elements.add(PathElements.index(Integer.parseInt(indexStr)));
			}
		} else {
			elements.add(PathElements.name(element));
		}
	}
	
	private static Path fromStringWithSplitter(String value, Splitter splitter) {
		if (value == null || value.isEmpty()) {
			return EMPTY_PATH;
		} else {
			String[] items = Iterables.toArray(splitter.split(value), String.class);
			if (items.length == 0) {
				return EMPTY_PATH;
			} else {
				return new Path(parse(items));
			}
		}
	}
	
	private static PathElement[] parse(String[] items) {
		List<PathElement> elements = new ArrayList<>();
		for (int i = 0; i < items.length; i++) {
			String item = items[i];
			if (item != null) {
				addElementTo(item, elements);
			}
		}
		
		return elements.toArray(new PathElement[elements.size()]);
	}
	
	public static Path empty() {
		return EMPTY_PATH;
	}
	
	public static Path root() {
		return EMPTY_PATH;
	}
	
	public static Path path(String... elements) {
		return new Path(parse(elements));
	}
	
	public static Path path(Iterator<PathElement> iterator) {
		if (!iterator.hasNext()) {
			return EMPTY_PATH;
		}
		return new Path(Iterators.toArray(iterator, PathElement.class));
	}
	
	public static Path fromHex(String hex) {
		return fromByteArray(HEX.decode(hex));
	}
	
	public static Path fromByteArray(byte[] bytes) {
		try {
			DataInput di = new DataInputStream(new ByteArrayInputStream(bytes));
			int size = di.readInt();
			
			if (size == 0) {
				return EMPTY_PATH;
			}
			
			PathElement[] elements = new PathElement[size];
			for (int i = 0; i < size; i++) {
				if (di.readBoolean()) {
					elements[i] = PathElements.name(di.readUTF());	
				} else {
					elements[i] = PathElements.index(di.readInt());
				}
			}
			return new Path(elements);
		} catch (Exception e) {
			throw unchecked(e);
		}
	}
	
	public static Path path(PathElement... elements) {
		return elements.length == 0 ? EMPTY_PATH : new Path(elements);
	}

	private Path(PathElement[] elements) {
		int emptyCount = 0;
		for (PathElement e : elements) {
			if (e.isEmpty()) emptyCount++;
		}
		if (emptyCount > 0) {
			this.elements = new PathElement[elements.length - emptyCount];
			int i = 0;
			for (PathElement e : elements) {
				if (!e.isEmpty()) {
					this.elements[i] = e;
					i++;
				}
			}
		} else {
			this.elements = elements;
		}
		this.size = this.elements.length;
	}
	
	private Path() {
		this(new PathElement[]{});
	}
	
	/*
	private Path(PathElement... elements) {
		this(elements);
	}
	*/
	
	public String wrapAndJoin(String wrap, String seperator) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			sb.append(wrap.replaceAll("\\{\\}", elements[i].toString()));
			if (i < elements.length - 1) {
				sb.append(seperator);
			}
		}
		return sb.toString();
	}
	
	public String join(String seperator) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			sb.append(elements[i]);
			if (i < elements.length - 1) {
				sb.append(seperator);
			}
		}
		return sb.toString();
	}
	
	public Path add(Path other) {
		PathElement[] newElements = new PathElement[elements.length + other.elements.length];
		System.arraycopy(elements, 0, newElements, 0, elements.length);
		System.arraycopy(other.elements, 0, newElements, elements.length, other.elements.length);
		return new Path(newElements);
	}
	
	public Path add(PathElement element) {
		PathElement[] newElements = new PathElement[elements.length + 1];
		System.arraycopy(elements, 0, newElements, 0, elements.length);
		newElements[newElements.length - 1] = element;
		return new Path(newElements);
	}
	
	public Path add(PathElement... es) {
		PathElement[] newElements = new PathElement[elements.length + es.length];
		System.arraycopy(elements, 0, newElements, 0, elements.length);
		System.arraycopy(es, 0, newElements, elements.length, es.length);
		return new Path(newElements);
	}
	
	public Path add(String element) {
		return add(PathElements.name(element));
	}
	
	public Path add(int element) {
		return add(PathElements.index(element));
	}
	
	public PathElement last() {
		return isEmpty() ? null : elements[elements.length - 1];
	}
	
	public Path butlast() {
		return size > 1 ? new Path(subList(elements, 0, elements.length - 1)) : empty(); 
	}
	
	private static PathElement[] subList(PathElement[] src, int fromIndex, int toIndex) {
		int len = toIndex - fromIndex;
		PathElement[] dest = new PathElement[len];
		System.arraycopy(src, fromIndex, dest, 0, len);
		return dest;
	}

	@Override
	public Iterator<PathElement> iterator() {
		return asList(elements).iterator();
	}

	public int length() {
		return size;
	}

	public PathElement get(int i) {
		return elements[i];
	}
	
	public byte[] toByteArray() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeInt(size);
			for (PathElement element : elements) {
				dos.writeBoolean(element.name() != null);
				if (element.name() != null) {
					dos.writeUTF(element.name());
				} else {
					dos.writeInt(element.index());
				}
			}
			dos.close();
			baos.close();
			return baos.toByteArray();
		} catch (Exception e) {
			throw unchecked(e);
		}
	}
	
	public String hex() {
		return HEX.encode(toByteArray());
	}
	
	public PathElement[] toArray() {
		return elements;
	}

	public Path subpath(int fromIndex) {
		return subpath(fromIndex, size);
	}
	
	public Path subpath(int fromIndex, int toIndex) {
		return new Path(subList(elements, fromIndex, toIndex));
	}

	public PathElement first() {
		return elements.length == 0 ? null : elements[0];
	}

	public boolean startsWith(Path other) {
		if (length() < other.length()) {
			return false;
		}
		for (int i = 0; i < other.length(); i++) {
			if (!other.get(i).equals(elements[i])) {
				return false;
			}
		}
		return true;
	}
	
	public boolean endsWith(Path other) {
		int otherLength = other.length();
		int thisLength = length();
		if (thisLength < otherLength) {
			return false;
		}
		int min = Math.min(thisLength, otherLength);
		for (int i = 1; i <= min; i++) {
			if (!other.get(otherLength - i).equals(elements[thisLength - i])) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(elements);
	}

	public boolean isEmpty() {
		return size == 0;
	}
	
	public String dots() {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < size; i++) {
			PathElement e = elements[i];
			if (e.isKey()) {
				b.append(e.name().replaceAll("\\.", "\\\\."));
			} else if (e.isNextIndex()) {
				b.append("[+]");
			} else {
				b.append(e);
			}
			if (i < size - 1 && elements[i + 1].isKey()) {
				b.append(".");
			}
		}
		return b.toString();
	}
	
	public String slashes() {
		return slashJoiner.join(elements);
	}
	
	public String url() {
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < size; i++) {
				PathElement element = elements[i];
				if (element.isKey()) {
					sb.append(URLEncoder.encode(element.name(), Charsets.UTF_8.name()));
				} else {
					sb.append(element.toString());
				}
				if (i < size - 1) {
					sb.append("/");
				}
			}
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw unchecked(e);
		}
	}
	
	private static <T> List<T> iteratorToList(List<T> list, Iterator<T> iterator) {
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}

	@Override
	public int compareTo(Path o) {
		if (o.equals(this)) {
			return 0;
		} else {
			int count = Math.min(size, o.size);
			for (int i = 0; i < count; i++) {
				int val = get(i).compareTo(o.get(i));
				if (val != 0) {
					return val;
				}
			}
			return size - o.size;
		}
	}

	@Override
	public Hasher hash(Hasher hasher) {
		hasher.putInt(size);
		for (PathElement e : elements) {
			e.hash(hasher);
		}
		return hasher;
	}

}
