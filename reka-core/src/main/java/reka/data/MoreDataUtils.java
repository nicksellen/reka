package reka.data;

import static reka.util.Util.unchecked;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

public class MoreDataUtils {

	private static final JsonFactory factory = new JsonFactory();
	
	public static <S extends OutputStream> S writeToOutputStreamAsJson(Data store, S stream) {
		return writeAsJsonToOutputStream(store, stream, false);
	}
	
	public static <S extends OutputStream> S writeToOutputStreamAsPrettyJson(Data store, S stream) {
		return writeAsJsonToOutputStream(store, stream, true);
	}

	public static void writeToFileAsPrettyJson(Data store, String filename) {
		writeToFileAsJson(store, new File(filename), true);
	}

	public static void writeToFileAsJson(Data store, String filename) {
		writeToFileAsJson(store, new File(filename), false);
	}

	private static void writeToFileAsJson(Data store, File file, boolean pretty) {
		try (FileOutputStream fis = new FileOutputStream(file)) {
			writeAsJsonToOutputStream(store, fis, true);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	private static <S extends OutputStream> S writeAsJsonToOutputStream(Data store, S stream, boolean pretty) {
		try {
			JsonGenerator json = factory.createJsonGenerator(stream);
			if (pretty) json.useDefaultPrettyPrinter();
			store.writeJsonTo(json);
			json.flush();
			return stream;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}
