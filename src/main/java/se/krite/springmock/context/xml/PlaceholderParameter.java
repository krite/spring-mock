package se.krite.springmock.context.xml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author kristoffer.teuber
 */
public class PlaceholderParameter {

	@XStreamAsAttribute
	@XStreamAlias("name")
	private String name;

	@XStreamAsAttribute
	@XStreamAlias("value")
	private String value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
