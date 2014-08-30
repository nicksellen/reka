package reka.config;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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
    public Path nestedFile(String location) {
    	
		Path other = file.toPath().getParent().resolve(location);
		if (isConstrained()) {
			if (!other.startsWith(constraint().toPath())) {
				log.error("tried to load nested file {} but we have a constraint {}", other, constrainTo.getAbsolutePath());
				throw new RuntimeException("illegal path!");
			}
		}
    	
        return file.toPath().getParent().resolve(location);
    }
    
    @Override
    public List<Path> nestedFiles(String location) {
    	
    	Path base = file.toPath().getParent();
    	
    	String glob = "glob:" + base.resolve(location).toAbsolutePath();
    	
    	log.info("finding files within {} that match {}", base.toAbsolutePath(), glob);
    	
    	PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
    	
    	List<Path> files = new ArrayList<>();
    	
    	try {
			Files.walkFileTree(base, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (matcher.matches(file)) files.add(base.relativize(file));
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					if (matcher.matches(dir)) {
						addAll(dir, files, base);
						return FileVisitResult.SKIP_SUBTREE;
					} else {
						return FileVisitResult.CONTINUE;
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	
    	files.sort((a, b) -> a.compareTo(b));
    	
    	return files;
    }

	@Override
    public boolean supportsNestedData() {
        return true;
    }

    @Override
    public byte[] nestedData(String location) {
    	try (FileInputStream fis = new FileInputStream(nestedFile(location).toFile())) {
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
	
	private static void addAll(Path dir, List<Path> files, Path base) {
	   try {
   			// grab everything inside here!
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>(){
				@Override
			    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					files.add(base.relativize(file));
			        return FileVisitResult.CONTINUE;
			    }
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
