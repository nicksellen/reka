package io.reka.net.tests;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import reka.net.NetModule;
import reka.util.RekaTest;

@RunWith(AllTests.class)
public class NetModuleTest extends TestCase {

	public static TestSuite suite() throws IOException, InterruptedException, ExecutionException {
		NetModule module = new NetModule();
		RekaTest.runApp(module, new File("src/test/resources/reka-tests/http-server.reka"));
		return RekaTest.createTestSuiteFrom(module, new File("src/test/resources/reka-tests/http.reka"));
	}

}