package reka.config.formatters;

import static reka.config.formatters.FormattingUtil.addIndent;

import java.util.ArrayDeque;
import java.util.Deque;

import reka.config.FormattingOptions;

import com.google.common.base.Charsets;

public class ConfigFormatter implements Formatter<String> {

    private final FormattingOptions opts;
	private final StringBuilder sb;
	
	private final Deque<ConfigFormatter.ObjectState> stack = new ArrayDeque<>();
	
	static class ObjectState {
		boolean somethingWasPadded = false;
		int idxStartEntry = 0;
		int idxStartObject = 0;
		int idxEntry = -1;
		boolean lastWasPadded = false;
		boolean firstWasPadded = false;
	}
	
	protected StringBuilder sb() {
	    return sb;
	}
	
	private ObjectState current() {
		return stack.peek();
	}
	
	public ConfigFormatter() {
	    this(new FormattingOptions());
	}
	
	private ConfigFormatter(FormattingOptions opts) {
	    this.opts = opts;
        this.sb = new StringBuilder();
        stack.push(new ObjectState());
	}

    @Override
    public void reset() {
        sb.delete(0, sb.length());
        stack.clear();
        stack.push(new ObjectState());
    }
	
	@Override
	public void comment(String text) {
		indent();
		sb.append("# ").append(text).append('\n');
	}

	@Override
	public void startEntry(String text, boolean hasBody) {
		current().idxStartEntry = sb.length();
		current().idxEntry++;
		indent();
		sb.append(text);
	}
	
	@Override
	public void endEntry() {
		sb.append('\n');
	}

	@Override
	public void value(String text) {
		sb.append(' ').append(text);
		current().lastWasPadded = false;
	}

	@Override
	public void document(String type, byte[] content) {
        current().somethingWasPadded = true;
		if (current().idxEntry == 0) current().firstWasPadded = true;
        if (!opts.compact() && !current().lastWasPadded) sb.insert(current().idxStartEntry, '\n');
		sb.append(" <<- ").append(type).append('\n')
			.append(documentContent(new String(content, Charsets.UTF_8))).append('\n');
		indent();
		sb.append("---");
		if (!opts.compact()) sb.append('\n');
		current().lastWasPadded = true;
	}

    @Override
    public String documentContent(String content) {
        return addIndent(stack.size(), content);
    }

	@Override
	public void importData(String type, String location) {
		sb.append(" < ");
		if (type != null) sb.append(type).append(' ');
		sb.append("@ ").append(location).append('\n');
	}

	@Override
	public void noChildren() {
		if (!opts.compact()) sb.insert(current().idxStartEntry, '\n');
		sb.append(" {\n");
		indent();
		sb.append("}");
	}
	
	@Override
	public void startChildren(int count) {
		current().somethingWasPadded = true;
		if (current().idxEntry == 0) current().firstWasPadded = true;
		if (!opts.compact() && !current().lastWasPadded) sb.insert(current().idxStartEntry, '\n');
		stack.push(new ObjectState());
		sb.append(" {\n");
		current().idxStartObject = sb.length();
	}

	@Override
	public void endChildren() {
		if (!opts.compact() && current().somethingWasPadded) {
			if (!current().firstWasPadded) sb.insert(current().idxStartObject, '\n');
			if (!current().lastWasPadded) sb.append('\n');
		}
        if (current().somethingWasPadded && !opts.compact()) sb.append('\n');
		stack.pop();
		indent();
		sb.append("}");
		current().lastWasPadded = true;
	}

	private void indent() {
		for (int i = 0; i < stack.size() - 1; i++) {
			sb.append("  ");
		}
	}

    protected int depth() {
        return stack.size();
    }
	
	@Override
	public String format() {
		return sb.toString();
	}
	
}