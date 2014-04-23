package reka.config.formatters;

public interface Formatter<T> {
    public void reset();
	public void comment(String text);
	public void startEntry(String key, boolean hasBody);
	public void endEntry();
	public void value(String text);
	public void document(String type, byte[] content);
    public String documentContent(String content);
	public void importData(String type, String location);
	public void noChildren();
	public void startChildren(int count);
	public void endChildren();
	public T format();
}