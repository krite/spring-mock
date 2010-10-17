package se.krite.springmock.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.test.context.ContextLoader;

/**
 * Custom context loader used to load context files using a standard format.
 * This context loader will rebuild the relative paths given to the @ContextConfiguration, to absolute paths
 * so that they can be located without any workspace specific settings.
 *
 * @author kristoffer.teuber
 */
public class TestContextLoader implements ContextLoader {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public ApplicationContext loadContext(String... locations) throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		this.log.debug("Building wew generic application context built...");
		// spring 3.0.X -> AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		AbstractBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.setResourceLoader(new FileSystemResourceLoader());
		reader.loadBeanDefinitions(locations);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		DefaultListableBeanFactory factory = context.getDefaultListableBeanFactory();
		factory.setInstantiationStrategy(new MockingAliasingCglibSubclassingInstantiationStrategy());
		context.addBeanFactoryPostProcessor(new MockingAliasingBeanFactoryPostProcessor());
		MockResourceAnnotationBeanPostProcessor.setBeanFactory(context.getBeanFactory());
		context.refresh();
		context.registerShutdownHook();
		this.log.debug("New generic application context built: " + context.getId());
		return context;
	}

	public String[] processLocations(Class<?> clazz, String... locations) {
		int i = 0;
		String[] finalLocations = new String[locations.length];
		for (String location : locations)
			finalLocations[i++] = location;
		return finalLocations;
	}
}
