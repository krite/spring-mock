package se.krite.springmock.context;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.lang.StringUtils;
import se.krite.springmock.context.xml.PlaceholderContext;
import se.krite.springmock.context.xml.PlaceholderParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Properties;

/**
 * Extension of the spring place holder functionality for servlet context properties
 * Since different environments seem to run test cases differently, we must use a hard file-url, based on
 * our current file's absolute path.
 * The context loader looks for the named file in the current path, and all the way up to the root
 * of the hard drive.
 *
 * @author kristoffer.teuber
 */
public class ContextXmlPropertyLookup {

	public static final String testContextFilePathParam = "test_context_file_name";

	private String contextFileName;
	private String testClassName;
	private Properties properties;
	private File contextConfigFile;

	public ContextXmlPropertyLookup(String contextFileName, String testClassName) {
		this.contextFileName = contextFileName;
		this.testClassName = testClassName;
		this.loadProperties();
	}

	public Properties getProperties() {
		return this.properties;
	}

	public String getProperty(String name) {
		return this.properties.getProperty(name, null);
	}

	public boolean getBooleanProperty(String name) {
		return "true".equals(this.properties.getProperty(name, null));
	}

	// If we are running from an ant compile structure, we will be in <projectroot>/WEB-INF/classes

	public String getTestBasePath() {
		String projectBasePath = this.properties.getProperty(ContextXmlPropertyLookup.testContextFilePathParam);
		if (projectBasePath == null)
			return null;
		projectBasePath = projectBasePath.replaceAll("\\\\", "/");
		projectBasePath = StringUtils.substringBeforeLast(projectBasePath, "/");
		if (StringUtils.substringAfterLast(projectBasePath, "/").equals("classes"))
			projectBasePath = StringUtils.substringBeforeLast(projectBasePath, "/");
		if (StringUtils.substringAfterLast(projectBasePath, "/").equals("WEB-INF"))
			projectBasePath = StringUtils.substringBeforeLast(projectBasePath, "/");
		System.out.println("Tests using base path: " + projectBasePath);
		return projectBasePath;
	}

	private void loadProperties() {
		PlaceholderContext context = this.loadFile();
		if (context == null)
			return;

		this.properties = new Properties();
		for (PlaceholderParameter parameter : context.getParameters()) {
			this.properties.put(parameter.getName(), parameter.getValue());
			System.out.println(parameter.getName() + " = " + parameter.getValue());
		}
		this.properties.put(testContextFilePathParam, this.contextConfigFile.getAbsolutePath());

		System.out.println("ContextXml placeholder: properties set!");
	}

	private PlaceholderContext loadFile() {
		try {
			this.contextConfigFile = this.findFile();
			XStream xstream = new XStream(new DomDriver());
			xstream.setClassLoader(this.getClass().getClassLoader());
			xstream.processAnnotations(new Class[]{PlaceholderContext.class, PlaceholderParameter.class});
			return (PlaceholderContext) xstream.fromXML(new FileInputStream(this.contextConfigFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public File getContextConfigFile() {
		return this.contextConfigFile;
	}

	public static File findFile(File directory, String fileName) {
		if (directory.isDirectory()) {
			for (File file : directory.listFiles()) {
				File result = findFile(file, fileName);
				if (result != null) {
					return result;
				}
			}
		} else if (directory.getName().equals(fileName)) {
			return directory;
		}
		return null;
	}

	/**
	 * Traverse file tree upwards, until file is found
	 */
	private File findFile() {
		String startDir = this.getFilePath();
		startDir = startDir.replaceAll("%20", " ");
		System.out.println("Path to start looking for config: " + startDir);
		return this.findFile(new File(startDir));
	}

	private File findFile(File directory) {
		if (directory.isDirectory()) {
			// Try to find file normally
			File file = new File(directory.getAbsoluteFile() + "/" + this.contextFileName);
			if (file.isFile()) {
				return file;
			}
			// Also try to look for file under current folder + /META-INF/<file-name>
			file = new File(directory.getAbsoluteFile() + "/META-INF/" + this.contextFileName);
			if (file.isFile()) {
				return file;
			}

			if (directory.getParentFile() != null)
				return findFile(directory.getParentFile());
			System.out.println("Could not find file: " + this.contextFileName);
			return null;
		} else
			return directory;
	}

	public String getFilePath() {
		return this.getFilePath(this.testClassName);
	}

	public String getFilePath(String className) {
		if (className == null)
			className = this.getClass().getName();
		if (!className.startsWith("/"))
			className = "/" + className;
		className = className.replace('.', '/');
		className += ".class";

		URL classUrl = this.getClass().getResource(className);
		if (classUrl != null) {
			String temp = classUrl.getFile();
			if (temp.startsWith("file:"))
				return temp.substring(5);

			return StringUtils.substringBeforeLast(StringUtils.substringBeforeLast(temp, "/"), "\\");
		}
		return null;
	}
}
