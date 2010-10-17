package se.krite.springmock.context;

import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * The base class for mocked integration tests.
 * This class is responsible for adding correct listeners to support the mocking framework.
 *
 * @author kristoffer.teuber
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = TestContextLoader.class)
@TransactionConfiguration(transactionManager = "txManager", defaultRollback = true)
@TestExecutionListeners({
		MockResourceTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class,
		TransactionalTestExecutionListener.class
})
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public abstract class TransactionalIntegrationTestBase extends TestCase {

	protected void assertNotNullOrEmpty(String s) {
		assertNotNull(s);
		assertFalse("Did not expect the string to be empty.", s.isEmpty());
	}

	protected void assertEmpty(Collection<?> c) {
		assertTrue("Collection is not empty. Contains " + c.size() + " items.", c.size() == 0);
	}

	protected void assertGreater(int expected, int actual) {
		assertTrue("Field was not greater than expectation. Expected greater than " +
				expected + " but was " + actual, expected > actual);
	}
}
