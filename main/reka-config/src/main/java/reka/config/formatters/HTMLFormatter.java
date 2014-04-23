package reka.config.formatters;


import org.apache.commons.lang3.StringEscapeUtils;

import com.google.common.base.Charsets;

public class HTMLFormatter implements Formatter<String> {

	private final StringBuilder sb = new StringBuilder();
	
	@Override
	public void comment(String text) {
		sb.append("<span class='comment'>").append(escape(text)).append("</span>\n");
	}

	@Override
	public void startEntry(String key, boolean hasBody) {
		sb.append("<li class='entry'>")
			.append("<strong class='key'>").append(escape(key)).append("</strong>");
	}

	@Override
	public void endEntry() {
		sb.append("</li>\n");
	}

	@Override
	public void value(String text) {
		sb.append(String.format("<span class='value'>%s</span>", escape(text)));
	}

	@Override
	public void document(String type, byte[] content) {
		sb.append(String.format(
			"<span class='heredoc'><span class='type'>%s</span><span class='content'>%s</span></span>\n",
				escape(type), escape(new String(content, Charsets.UTF_8))));
	}

    @Override
    public String documentContent(String content) {
        return content;
    }

	@Override
	public void importData(String type, String location) {
		sb.append(String.format("<span class='import-data'>%s</span>\n", escape(location)));
	}

	@Override
	public void noChildren() {
		
	}

	@Override
	public void startChildren(int count) {
		sb.append("<ul>");
	}

	@Override
	public void endChildren() {
		sb.append("</ul>");
	}

	@Override
	public String format() {
		return String.format("<ul>%s</ul>", sb.toString());
	}
	
	private String escape(String input) {
		return StringEscapeUtils.escapeHtml4(input);
	}

    @Override
    public void reset() {
        sb.delete(0, sb.length());
    }
	
}