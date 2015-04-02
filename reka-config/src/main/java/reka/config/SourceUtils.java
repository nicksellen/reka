package reka.config;


public class SourceUtils {
	
	public static SourceLinenumbers linenumbersFor(String content) {
    	return new SourceLinenumbers(1, 1, occurances(content, '\n') + 1, fromend(content, '\n') + 1);
	}
    
    public static int occurances(CharSequence s, char t) {
    	int count = 0;
    	for (int i = 0; i < s.length(); i++) {
    		if (s.charAt(i) == t) count++;
    	}
    	return count;
    }
    
    public static int fromend(CharSequence s, char t) {
    	int len = s.length(), i = 0;
    	for (i = 0; i < len; i++) {
    		if (s.charAt(len - 1 - i) == t) return i;
    	}
    	return i;
    }

}
