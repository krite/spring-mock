package se.krite.springmock.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kristoffer.teuber
 */
public class MyTestContextLoader extends TestContextLoader {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String testContextFileName = "testContext.xml";
	private String[] defaultLocations = new String[]{
			"applicationContextTest.xml",
			"applicationContext-db.xml"};
	private String projectBasePath;

	public MyTestContextLoader() {
		ContextXmlPropertyLookup cxpl = new ContextXmlPropertyLookup(this.testContextFileName, this.getClass().getName());
		boolean runningOnServer = cxpl.getBooleanProperty("run_tests_on_server");
		boolean runningInPlace = cxpl.getBooleanProperty("run_tests_in_place");

		if (!runningOnServer) {
			// If tests are run in intellij or eclipse, we use a locally statically configured path to
			// where the project is placed. This is because the entire app is not deployed when tests are run
			String pathName = "test_context_base_path";
			String appendToLocationPath = "file:";
			if (!runningInPlace) {
				pathName += "_ant";
			} else {
				appendToLocationPath += "web/";
			}
			appendToLocationPath += "WEB-INF/";
			for (int i = 0; i < this.defaultLocations.length; i++) {
				this.defaultLocations[i] = appendToLocationPath + this.defaultLocations[i];
			}
			this.projectBasePath = cxpl.getProperties().getProperty(pathName);

		} else {
			// If tests are run by ant on server, hudson etc., we will find path by testContext.xml location.
			this.projectBasePath = cxpl.getTestBasePath();
			String appendToLocationPath = "file:WEB-INF/";
			for (int i = 0; i < this.defaultLocations.length; i++) {
				this.defaultLocations[i] = appendToLocationPath + this.defaultLocations[i];
			}
		}
		if (!this.projectBasePath.endsWith("/"))
			this.projectBasePath += "/";

		log.info("Project base path set to: " + this.projectBasePath);
	}

	private String getAbsoluteFilePath(String path) {
		String base = "";
		if (path.startsWith("file:")) {
			path = path.substring(5);
			base = "file:";
		}
		String filePath = base + this.projectBasePath + path;
		log.info("File path fetched: " + filePath);
		return filePath;
	}

	public String[] processLocations(Class<?> clazz, String... locations) {
		int i = 0;
		String[] finalLocations = new String[locations.length + this.defaultLocations.length];
		for (String location : locations)
			finalLocations[i++] = this.getAbsoluteFilePath(location);
		for (String location : this.defaultLocations)
			finalLocations[i++] = this.getAbsoluteFilePath(location);
		return finalLocations;
	}
}
