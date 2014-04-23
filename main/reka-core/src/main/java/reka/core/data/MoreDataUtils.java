package reka.core.data;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import reka.api.data.Data;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.base.Charsets;

public class MoreDataUtils {

	private static final JsonFactory factory = new JsonFactory();
	
	@Deprecated
	public static Data readFromBytesAsJson(byte[] json) {
		try {
			return MutableMemoryData.readJson(factory.createJsonParser(json));
		} catch (IOException e) {
			throw unchecked(e);
		}
	}


	@Deprecated
	public static Data readFromStringAsJson(String json) {
		try {
			return MutableMemoryData.readJson(factory.createJsonParser(json));
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	@Deprecated
	public static Data readFromStreamAsJson(InputStream json) {
		try {
			return MutableMemoryData.readJson(factory.createJsonParser(json));
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	@Deprecated
	public static String writeToStringAsPrettyJson(Data store) {
		return writeToStringAsJson(store, true);
	}

	@Deprecated
	public static String writeToStringAsJson(Data store) {
		return writeToStringAsJson(store, false);
	}

	@Deprecated
	private static String writeToStringAsJson(Data store, boolean pretty) {
		return new String(writeAsJsonToOutputStream(store, 
			new ByteArrayOutputStream(), pretty).toByteArray(), Charsets.UTF_8);
	}

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
			store.out(json);
			json.flush();
			return stream;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}
