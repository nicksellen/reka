package reka.config;

import static java.lang.String.format;

public class SourceLinenumbers {
	
	private final int startLine;
	private final int startPos;
	
	private final int endLine;
	private final int endPos;
	
	public SourceLinenumbers(int startLine, int startPos, int endLine,int endPos) {
		this.startLine = startLine;
		this.startPos = startPos;
		this.endLine = endLine;
		this.endPos = endPos;
	}

	public int startLine() {
		return startLine;
	}

	public int startPos() {
		return startPos;
	}

	public int endLine() {
		return endLine;
	}

	public int endPos() {
		return endPos;
	}
	
	@Override
	public String toString() {
		return format("%s:%s-%s:%s", startLine, startPos, endLine, endPos);
	}

}
