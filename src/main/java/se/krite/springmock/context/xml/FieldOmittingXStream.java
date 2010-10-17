package se.krite.springmock.context.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * An overridden XStream implementation that omits all fields
 * that are not mapped by an XStream-annotation
 *
 * @author kristoffer.teuber
 */
public class FieldOmittingXStream extends XStream {
	private Logger log = LoggerFactory.getLogger(getClass());

	public FieldOmittingXStream() {
		super();
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider,
								HierarchicalStreamDriver hierarchicalStreamDriver) {
		super(reflectionProvider, hierarchicalStreamDriver);
	}

	public FieldOmittingXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
		super(hierarchicalStreamDriver);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider) {
		super(reflectionProvider);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
								ClassLoader classLoader,
								Mapper mapper, ConverterLookup converterLookup, ConverterRegistry converterRegistry) {
		super(reflectionProvider, driver, classLoader, mapper, converterLookup, converterRegistry);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
								ClassLoader classLoader,
								Mapper mapper) {
		super(reflectionProvider, driver, classLoader, mapper);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
								ClassLoader classLoader) {
		super(reflectionProvider, driver, classLoader);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider, Mapper mapper, HierarchicalStreamDriver driver) {
		super(reflectionProvider, mapper, driver);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider, ClassMapper classMapper,
								HierarchicalStreamDriver driver,
								String classAttributeIdentifier) {
		super(reflectionProvider, classMapper, driver, classAttributeIdentifier);
	}

	public FieldOmittingXStream(ReflectionProvider reflectionProvider, ClassMapper classMapper,
								HierarchicalStreamDriver driver) {
		super(reflectionProvider, classMapper, driver);
	}

	@Override
	protected MapperWrapper wrapMapper(MapperWrapper next) {
		return new MapperWrapper(next) {
			@Override
			public boolean shouldSerializeMember(Class definedIn, String fieldName) {
				try {
					Field field = definedIn.getDeclaredField(fieldName);
					for (Annotation annotation : field.getDeclaredAnnotations()) {
						String name = annotation.toString();
						name = StringUtils.substringBefore(name, "(");
						name = StringUtils.substringAfterLast(name, ".");
						if (name.startsWith("XStream") && !name.equals("XStreamOmitField")) {
							return true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		};
	}
}
