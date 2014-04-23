package reka.config.formatters;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.util.ArrayDeque;
import java.util.Deque;

import com.google.common.base.Charsets;

public class HTMLSyntaxFormatter extends ConfigFormatter {

    private final StringBuffer toc = new StringBuffer();
    
	private final Deque<String> path = new ArrayDeque<>();
	private String key;
	private String val;
	
	private int id = 0;

    private String tagOpen(String tag, String... classNames) {
        StringBuilder classes = new StringBuilder();
        for (int i = 0; i < classNames.length; i++) {
            classes.append(classNames[i]);
            classes.append(' ');
        }
        return String.format("<%s class='%s'>", tag, classes.toString().trim());
    }

    private String tagClose(String tag) {
        return String.format("</%s>", tag);
    }
    
    private String classNames(String... classNames) {
    	StringBuilder classes = new StringBuilder();
        for (int i = 0; i < classNames.length; i++) {
            classes.append(classNames[i]);
            classes.append(' ');
        }
        return classes.toString().trim();
    }

    private String tagUnescaped(String tag, String content, String... classNames) {
        return String.format("<%s class='%s'>%s</%s>", tag, classNames(classNames), content, tag);
    }

	private String tag(String tag, String content, String... classNames) {
        return tagUnescaped(tag, escapeHtml4(content), classNames);
	}
	
	private String span(String content, String... classes) {
		return tag("span", content, classes);
	}

    private String spanOpen(String... classes) {
        return tagOpen("span", classes);
    }

    private String spanClose() {
        return tagClose("span");
    }

	@Override
	public void startEntry(String text, boolean hasBody) {
	    id++;
	    key = text;
	    val = null;
	    sb().append(String.format("<span id='node-%s' data-node='%s' class='node %s %s'>", id, text, text, hasBody ? "object" : ""));
		path.push(text);
		super.startEntry(span(text, "key", pathClasses()), hasBody);
	}

	@Override
	public void endEntry() {
		path.pop();
		super.endEntry();
		sb().append(spanClose());
        toc.append("</li>");
	}

	@Override
	public void comment(String text) {
		super.comment(span(text, "comment"));
	}

	@Override
    public void startChildren(int count) {
	    toc.append("<li>");
	    toc.append("<a href='#node-").append(id).append("' class='depth-").append(depth()).append("'>");
	    toc.append(spanOpen("key"));
        toc.append(key);
        toc.append(spanClose());
        if (val != null) {
            toc.append(" ").append("<span class='val'>").append(val).append("</span>");
        }
        toc.append(tagClose("a"));
	    toc.append(tagOpen("ul"));

        sb().append(spanOpen("brace"));
        super.startChildren(count);
        sb().append(spanClose());
    }

    @Override
    public void endChildren() {
        sb().append(spanOpen("brace"));
        super.endChildren();
        sb().append(spanClose());
        toc.append("</ul>");
        toc.append("</li>");
    }

    @Override
	public void value(String text) {
        val = text;
		super.value(span(text, "val", pathClasses()));
	}

	@Override
	public void document(String type, byte[] content) {
		super.document(span(type, "heredoc-type"),
                      code(heredocTypeToLang(type), new String(content, Charsets.UTF_8), 
                    		  "heredoc-content",
                              "language-" + heredocTypeToLang(type),
                              String.format("heredoc-content-%s", type)).getBytes(Charsets.UTF_8));
	}
	
	private String code(String lang, String content, String... classes) {
		return String.format("<code data-lang='%s' class='%s'>%s</code>", lang, classNames(classes), escapeHtml4(content));
	}

    @Override
    public String documentContent(String content) {
        return content;
    }

    private String heredocTypeToLang(String type) {
    	if (type.contains("/")) {
    		String[] split = type.split("/");
    		type = split[split.length - 1];
    	}
        switch (type) {
            case "js":
            case "json":
                return "javascript";
            case "mustache":
            	return "handlebars";
            default:
            	return type;
        }
    }

	@Override
	public String format() {
		return String.format("<div class='config-toc'><ul>%s</ul></div><div class='config'>%s</div>", toc.toString(), super.format());
	}

    private String pathClasses() {
        return "";
    }
    /*
	private String pathClasses() {
		StringBuilder sb = new StringBuilder();
		for (String item : path) {
			if (item.equals(path.getFirst())) {
				sb.append(" at-");
			} else {
				sb.append(" within-");
			}
			sb.append(item);
		}
		return sb.toString().trim();
	}
	*/

    @Override
    public void reset() {
        super.reset();
        toc.delete(0, toc.length());
    }

}
