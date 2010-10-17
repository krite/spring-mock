package se.krite.springmock.context.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.JDomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

import java.io.InputStream;

/**
 * (currently) Static utility class for registering and converting classes to and from xml
 *
 * @author kristoffer.teuber
 */
public class XStreamHelper {

	private static final Class[] annotatedClasses =
			new Class[]{};


	private static XStreamHelper instance;
	private static final XmlFriendlyReplacer replacer = new XmlFriendlyReplacer("_", "_");
	private final XStream xstreamJDom;
	private final XStream xstreamText;
	private final XStream xstreamTextWithISOHeader;

	private XStreamHelper() {
		// JDom-serializer
		this.xstreamJDom = new FieldOmittingXStream(new JDomDriver(replacer));
		this.xstreamJDom.setClassLoader(this.getClass().getClassLoader());
		this.xstreamJDom.setMode(XStream.NO_REFERENCES);
		this.xstreamJDom.processAnnotations(annotatedClasses);

		// Text-serializer
		this.xstreamText = new FieldOmittingXStream(new NewlineDomDriver("UTF-8", "\r\n"));
		this.xstreamText.setClassLoader(this.getClass().getClassLoader());
		this.xstreamText.setMode(XStream.NO_REFERENCES);
		this.xstreamText.processAnnotations(annotatedClasses);

		// Text-serializer
		this.xstreamTextWithISOHeader = new FieldOmittingXStream(new NewlineDomDriver("ISO-8859-1", "\r\n", true));
		this.xstreamTextWithISOHeader.setClassLoader(this.getClass().getClassLoader());
		this.xstreamTextWithISOHeader.setMode(XStream.NO_REFERENCES);
		this.xstreamTextWithISOHeader.processAnnotations(annotatedClasses);
	}

	public static synchronized XStreamHelper getInstance() {
		if (instance == null) {
			instance = new XStreamHelper();
		}
		return instance;
	}

	public String marshalToXml(Object object) {
		return this.xstreamText.toXML(object);
	}

	public String marshalToXmlWithISOHeader(Object object) {
		return this.xstreamTextWithISOHeader.toXML(object);
	}

	public <K> K unmarshalXml(InputStream xml, Class<K> c) {
		try {
			return (K) xstreamText.fromXML(xml);
		} catch (Exception e) {
			return null;
		}
	}
}
