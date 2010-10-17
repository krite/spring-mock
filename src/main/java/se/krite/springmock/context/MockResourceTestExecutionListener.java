package se.krite.springmock.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Execution listener that harvests mock overrides from test cases
 *
 * @author kristoffer.teuber
 */
public class MockResourceTestExecutionListener extends AbstractTestExecutionListener {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void prepareTestInstance(final TestContext testContext) throws Exception {
		this.log.debug("Preparing new test class: " + testContext.getTestClass().getName());
		MockingClassLoader.registerAliases(testContext);
		MockingClassLoader.modifyMockingProxies(testContext);
	}

	@Override
	public void beforeTestMethod(final TestContext testContext) throws Exception {
		this.log.debug("Preparing new test method: " + testContext.getTestMethod().getName());
		MockingClassLoader.resetAllProxiesToDefaultValues(testContext);
		MockingClassLoader.modifyMockingProxies(testContext);
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		this.log.debug("Cleaning up after test method: " + testContext.getTestMethod().getName());
		MockingClassLoader.resetAllProxiesToDefaultValues(testContext);
	}
}