package reka.config;

import java.io.File;

public interface Source {

    public String content();
    
    public String location();
    
    public boolean isFile();
    public File file();

    public boolean supportsNestedFile();
    public File nestedFile(String location);
    
    public boolean supportsNestedData();
    public byte[] nestedData(String location);
    
    public boolean hasParent();
    public Source parent();
    
    public Source origin();
    public Source rootOrigin();
    
    public int originOffsetStart();
    public int originOffsetLength();
    
    public SourceLinenumbers linenumbers();
    
}
