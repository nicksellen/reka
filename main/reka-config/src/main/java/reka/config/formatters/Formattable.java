package reka.config.formatters;

import reka.config.FormattingOptions;

public interface Formattable {
    public <T> T format(Formatter<T> out, FormattingOptions opts);
    public <T> T format(Formatter<T> out);
    public String format(FormattingOptions opts);
    public String format();
}
