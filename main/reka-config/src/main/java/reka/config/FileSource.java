package reka.config;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import com.google.common.io.ByteStreams;

public class FileSource extends AbstractSource {

    private final File file;
    private final Source parent;
    
    public static Source from(File file) {
        return new FileSource(file, null);
    }

    public static Source from(File file, Source parent) {
        return new FileSource(file, parent);
    }
    
    private FileSource(File file, Source parent) {
        this.file = file;
        this.parent = parent;
    }
    
    @Override
    public Source origin() {
    	return this;
    }
    
    @Override
    public String content() {
        try {
            return new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public File file() {
        return file;
    }

    @Override
    public boolean hasParent() {
        return parent != null;
    }

    @Override
    public Source parent() {
        return parent;
    }
    
    @Override
    public String toString() {
    	return format("%s(%s)", getClass().getSimpleName(), file.getAbsolutePath());
    }

    @Override
    public boolean supportsNestedFile() {
        return true;
    }

    @Override
    public File nestedFile(String location) {
        return file.toPath().getParent().resolve(location).toFile();
    }

    @Override
    public boolean supportsNestedData() {
        return true;
    }

    @Override
    public byte[] nestedData(String location) {
    	try (FileInputStream fis = new FileInputStream(nestedFile(location));) {
    		return ByteStreams.toByteArray(fis);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		}
    }

    @Override
    public String location() {
        return file.getAbsolutePath();
    }

}
