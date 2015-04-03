package reka.modules.filesystem;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static reka.data.content.Contents.binary;
import static reka.data.content.Contents.longValue;
import static reka.modules.filesystem.FilesystemUtils.resolveAndCheck;
import static reka.util.Util.runtime;

import java.io.File;
import java.io.InputStream;
import java.util.function.Function;

import javax.activation.MimetypesFileTypeMap;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;
import reka.util.Path.Response;

public class FilesystemRead implements Operation {
	
	private final java.nio.file.Path basedir;
	private final Function<Data,Path> dataPathFn;
	private final Function<Data,String> filenameFn;
	private final boolean download;
	private final MimetypesFileTypeMap mimeTypesMap;
	
	public FilesystemRead(java.nio.file.Path basedir, Function<Data,Path> dataPathFn, Function<Data,String> filenameFn, boolean download) {
		this.basedir = basedir;
		this.dataPathFn = dataPathFn;
		this.filenameFn = filenameFn;
		this.download = download;
		InputStream mimeTypes = getClass().getResourceAsStream("/META-INF/mimetypes.default");
		checkNotNull(mimeTypes, "couldn't find mime.types on the resource path");
		mimeTypesMap = new MimetypesFileTypeMap(mimeTypes);
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		
		Path dataOut = dataPathFn.apply(data);
		
		String filename = filenameFn.apply(data);
		
		File file = resolveAndCheck(basedir, filename).toFile();
		
		if (file.exists() && file.isFile()) {
			
			String contentType = mimeTypesMap.getContentType(file.getPath());
			
			if (dataOut.equals(Response.CONTENT)) {
				
		        data.putString(Response.Headers.CONTENT_TYPE, contentType)
		        	.put(Response.Headers.CONTENT_LENGTH, longValue(file.length()))
		        	.put(Response.CONTENT, binary(contentType, file));
	
		        if (download) {
		        	
		        	data.putString(Response.Headers.CONTENT_DISPOSITION, 
		        			       format("attachment; filename=\"%s\"", file.getName()));
		        	
		        }
	        
			} else {
				
				data.put(dataOut, binary(contentType, file));
				
			}
			
		} else {
			throw runtime("%s cannot be read", file.getAbsolutePath());
		}
	}

}
