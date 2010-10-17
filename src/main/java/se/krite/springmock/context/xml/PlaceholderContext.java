package se.krite.springmock.context.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

/**
 * @author kristoffer.teuber
 */
@XStreamAlias("Context")
public class PlaceholderContext {

	@XStreamImplicit
	@XStreamAlias("Parameter")
	private List<PlaceholderParameter> parameters;

	public List<PlaceholderParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<PlaceholderParameter> parameters) {
		this.parameters = parameters;
	}
}

