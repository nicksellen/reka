package reka.test;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import reka.util.RekaTest;

@RunWith(AllTests.class)
public class RekaCoreTest extends TestCase {

	public static TestSuite suite() {
		return RekaTest.createTestSuiteFrom(null,
			new File("src/test/resources/reka-tests/builtins.reka"),
			new File("src/test/resources/reka-tests/json.reka"));
	}

}