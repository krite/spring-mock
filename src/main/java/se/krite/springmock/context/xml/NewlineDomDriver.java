package se.krite.springmock.context.xml;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.AbstractXmlDriver;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * Extends PrettyPrintWriter to support correct newlines
 *
 * @author kristoffer.teuber
 */
public class NewlineDomDriver extends AbstractXmlDriver {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String encoding;
	private final DocumentBuilderFactory documentBuilderFactory;
	private String newLine = null;
	private boolean addXmlHeader;

	/**
	 * Construct a DomDriver.
	 */
	public NewlineDomDriver() {
		this(null);
	}

	/**
	 * Construct a DomDriver with a specified encoding. The created DomReader will ignore any
	 * encoding attribute of the XML header though.
	 */
	public NewlineDomDriver(String encoding) {
		this(encoding, new XmlFriendlyReplacer());
	}

	public NewlineDomDriver(String encoding, String newLine) {
		this(encoding, newLine, false);
	}

	public NewlineDomDriver(String encoding, String newLine, boolean addXmlHeader) {
		this(encoding, new XmlFriendlyReplacer());
		this.newLine = newLine;
		this.addXmlHeader = addXmlHeader;
	}

	/**
	 * @since 1.2
	 */
	public NewlineDomDriver(String encoding, XmlFriendlyReplacer replacer) {
		super(replacer);
		documentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.encoding = encoding;
	}

	public HierarchicalStreamReader createReader(Reader xml) {
		return createReader(new InputSource(xml));
	}

	public HierarchicalStreamReader createReader(InputStream xml) {
		return createReader(new InputSource(xml));
	}

	private HierarchicalStreamReader createReader(InputSource source) {
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			if (encoding != null) {
				source.setEncoding(encoding);
			}
			Document document = documentBuilder.parse(source);
			return new DomReader(document, xmlFriendlyReplacer());
		} catch (FactoryConfigurationError e) {
			throw new StreamException(e);
		} catch (ParserConfigurationException e) {
			throw new StreamException(e);
		} catch (SAXException e) {
			throw new StreamException(e);
		} catch (IOException e) {
			throw new StreamException(e);
		}
	}

	public HierarchicalStreamWriter createWriter(Writer out) {
		// return new PrettyPrintWriter(out, xmlFriendlyReplacer());
		if (this.addXmlHeader) {
			try {
				out.write("<?xml version=\"1.0\" encoding=\"" + this.encoding + "\"?>" + this.newLine);
			} catch (IOException e) {
				log.error("Could not add xml-header to document", e);
			}
		}
		char[] indenter = new char[]{' ', ' '};
		XmlFriendlyReplacer replacer = new XmlFriendlyReplacer("_", "_");
		return new PrettyPrintWriter(out, indenter, this.newLine, replacer);
		//return new PrettyPrintWriter(out, PrettyPrintWriter.XML_1_1, indenter, xmlFriendlyReplacer());
	}

	public HierarchicalStreamWriter createWriter(OutputStream out) {
		try {
			return createWriter(encoding != null
					? new OutputStreamWriter(out, encoding)
					: new OutputStreamWriter(out));
		} catch (UnsupportedEncodingException e) {
			throw new StreamException(e);
		}
	}
}
