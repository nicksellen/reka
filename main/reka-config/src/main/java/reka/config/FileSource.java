package reka.config;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public class FileSource extends AbstractSource {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    private final File file;
    private final Source parent;
    private final File constrainTo;
    
    public static Source from(File file) {
        return new FileSource(file, file.getParentFile(), null);
    }
    
    public static Source from(File file, File constrainTo) {
        return new FileSource(file, constrainTo, null);
    }

    public static Source from(File file, File constrainTo, Source parent) {
        return new FileSource(file, constrainTo, parent);
    }
    
    private FileSource(File file, File constrainTo, Source parent) {
        this.file = file;
        this.constrainTo = constrainTo;
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
    	
		Path other = file.toPath().getParent().resolve(location);
		if (isConstrained()) {
			if (!other.startsWith(constraint().toPath())) {
				log.error("tried to load nested file {} but we have a constraint {}", other, constrainTo.getAbsolutePath());
				throw new RuntimeException("illegal path!");
			}
		}
    	
        return file.toPath().getParent().resolve(location).toFile();
    }

    @Override
    public boolean supportsNestedData() {
        return true;
    }

    @Override
    public byte[] nestedData(String location) {
    	try (FileInputStream fis = new FileInputStream(nestedFile(location))) {
    		return ByteStreams.toByteArray(fis);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		}
    }

    @Override
    public String location() {
        return file.getAbsolutePath();
    }

	@Override
	public boolean isConstrained() {
		return constrainTo != null;
	}

	@Override
	public File constraint() {
		return constrainTo;
	}

}
