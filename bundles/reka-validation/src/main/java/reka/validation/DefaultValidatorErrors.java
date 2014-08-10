package reka.validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path;

public class DefaultValidatorErrors implements ValidatorErrors, Iterable<Entry<Path,List<String>>> {
	
	private final Map<Path,List<String>> errors = new LinkedHashMap<>();

	@Override
	public void add(Path path, String msg) {
		List<String> msgs = errors.get(path);
		if (msgs == null) {
			msgs = new ArrayList<>();
			errors.put(path, msgs);
		}
		msgs.add(msg);
	}

	@Override
	public Iterator<Entry<Path, List<String>>> iterator() {
		return errors.entrySet().iterator();
	}

}
