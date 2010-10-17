package se.krite.springmock.context.xml;

import se.krite.springmock.context.ContextXmlPropertyLookup;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

/**
 * Used to obtain servlet context variables when application has no servlet context.
 *
 * @author: kristoffer.teuber
 */
public class MockingTestServletContext implements ServletContext {

	private ContextXmlPropertyLookup contextXmlPropertyLookup;

	public MockingTestServletContext(ContextXmlPropertyLookup contextXmlPropertyLookup) {
		this.contextXmlPropertyLookup = contextXmlPropertyLookup;
	}

	public String getInitParameter(String s) {
		if (this.contextXmlPropertyLookup == null)
			return null;
		return this.contextXmlPropertyLookup.getProperty(s);
	}

	public Enumeration getInitParameterNames() {
		if (this.contextXmlPropertyLookup == null)
			return null;
		return this.contextXmlPropertyLookup.getProperties().propertyNames();
	}

	public String getRealPath(String s) {
		File contextConfigFile = this.contextXmlPropertyLookup.getContextConfigFile();
		if (contextConfigFile == null) {
			return null;
		}

		boolean runningOnServer = this.contextXmlPropertyLookup.getBooleanProperty("run_tests_on_server");
		boolean runningInPlace = this.contextXmlPropertyLookup.getBooleanProperty("run_tests_in_place");

		String pathName = "test_context_base_path";
		if (!runningInPlace) {
			pathName += "_ant";
		}

		String path = this.contextXmlPropertyLookup.getProperty(pathName);
		if (runningOnServer) {
			path = this.contextXmlPropertyLookup.getTestBasePath();

		} else if (runningInPlace) {
			if (!path.endsWith("/")) {
				path += "/";
			}
			path += "web";
		}
		return path + s;
	}

	// Not used

	public String getContextPath() {
		return null;
	}

	public ServletContext getContext(String s) {
		return null;
	}

	public int getMajorVersion() {
		return 0;
	}

	public int getMinorVersion() {
		return 0;
	}

	public String getMimeType(String s) {
		return null;
	}

	public Set getResourcePaths(String s) {
		return null;
	}

	public URL getResource(String s) throws MalformedURLException {
		return null;
	}

	public InputStream getResourceAsStream(String s) {
		return null;
	}

	public RequestDispatcher getRequestDispatcher(String s) {
		return null;
	}

	public RequestDispatcher getNamedDispatcher(String s) {
		return null;
	}

	public Servlet getServlet(String s) throws ServletException {
		return null;
	}

	public Enumeration getServlets() {
		return null;
	}

	public Enumeration getServletNames() {
		return null;
	}

	public void log(String s) {

	}

	public void log(Exception e, String s) {

	}

	public void log(String s, Throwable throwable) {

	}

	public String getServerInfo() {
		return null;
	}

	public Object getAttribute(String s) {
		return null;
	}

	public Enumeration getAttributeNames() {
		return null;
	}

	public void setAttribute(String s, Object o) {

	}

	public void removeAttribute(String s) {

	}

	public String getServletContextName() {
		return null;
	}
}
