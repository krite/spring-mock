package se.krite.springmock.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.List;

/**
 * Bean post processor that adds the actual implementations used by the MockResource framework to
 * support original implementations as well as mocking implementations
 *
 * @author kristoffer.teuber
 */
public class MockingAliasingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public MockingAliasingBeanFactoryPostProcessor() {
		log.debug("MockingAliasingBeanFactoryPostProcessor initialized!");
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		log.debug("Starting to registering actual bean definitions...");
		if (beanFactory instanceof BeanDefinitionRegistry) {
			List<BeanDefinitionHolder> actualImplementations =
					MockingClassLoader.getActualImplementationBeans(beanFactory);
			for (BeanDefinitionHolder bdh : actualImplementations) {
				log.debug("Registering actual bean definition for bean: " + bdh.getBeanName() +
						" -> " + bdh.getBeanDefinition().getBeanClassName());
				((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(bdh.getBeanName(),
						bdh.getBeanDefinition());
			}
		}
		log.debug("Done registering actual bean definitions!");
	}
}

