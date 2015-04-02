package reka.config;

public class FormattingOptions {
	
	private boolean compact = false;
	
	public FormattingOptions compact(boolean val) {
		compact = val;
		return this;
	}
	
	public boolean compact() {
		return compact;
	}
	
	public FormattingOptions copy() {
		return new FormattingOptions().compact(compact);
	}
	
}