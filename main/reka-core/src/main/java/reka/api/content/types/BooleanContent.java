package reka.api.content.types;

import java.io.DataOutput;
import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;

import reka.api.content.Content;

import com.google.common.hash.Hasher;

public class BooleanContent implements Content {
	
	public static BooleanContent of(boolean val) {
		return val ? TRUE : FALSE;
	}
	
	public static final BooleanContent FALSE = new BooleanContent(false);
	public static final BooleanContent TRUE = new BooleanContent(true);

	private final boolean val;
	
	private BooleanContent(boolean val) {
		this.val = val;
	}

	@Override
	public Type type() {
		return Type.BOOLEAN;
	}

	@Override
	public void writeJsonTo(JsonGenerator json) throws IOException {
		json.writeBoolean(val);
	}

	@Override
	public void out(DataOutput out) throws IOException {
	}

	@Override
	public Object value() {
		return val;
	}

	@Override
	public Hasher hash(Hasher hasher) {
		return hasher.putBoolean(val);
	}
	
}