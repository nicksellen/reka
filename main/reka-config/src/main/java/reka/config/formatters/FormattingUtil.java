package reka.config.formatters;

import static com.google.common.collect.Iterables.toArray;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

public class FormattingUtil {
	
	public static String addIndent(int indent, String val) {
		String[] lines = val.split("\n", Integer.MAX_VALUE);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			for (int j = 0; j < indent; j++) {
				sb.append("  ");
			}
			sb.append(lines[i]);
			if (i < lines.length - 1) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}
	
	/*
	public static String addIndent(String indent, String val) {
		String[] lines = val.split("\n", Integer.MAX_VALUE);
		if (lines.length == 0) {
			return indent + val;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			sb.append(indent);
			sb.append(lines[i]);
			if (i < lines.length - 1) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}
	*/

    private static final Pattern leadingWS = Pattern.compile("^([\\ \t]+)\\S+");

    private static final String SPACE_INDENT = "    ";
    private static final Splitter linesplitter = Splitter.on("\n");

    public static String removeIndentation(String val) {
    	if (val.length() > 0 && val.charAt(0) == '\n') val = val.substring(1);
        String[] lines = toArray(linesplitter.split(val.replaceAll("\t", SPACE_INDENT)), String.class);
        String indent = null;
        for (String line : lines) {
            Matcher m = leadingWS.matcher(line);
            if (m.find()) {
                String s = m.group(1);
                if (indent == null) {
                    indent = s;
                } else if (s.length() < indent.length()) {
                    indent = s;
                }
            }
        }
        if (indent == null) {
            return val; // original
        }
        String pp = Pattern.quote(indent);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].replaceFirst("^" + pp, ""));
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
