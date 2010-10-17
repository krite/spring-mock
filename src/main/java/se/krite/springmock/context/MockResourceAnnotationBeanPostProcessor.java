package se.krite.springmock.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Annotation bean post processor that collects annotations for setting up the mocking alias names for beans
 * during junit test running.
 * Not currently used
 *
 * @author kristoffer.teuber
 */
public class MockResourceAnnotationBeanPostProcessor extends
		InstantiationAwareBeanPostProcessorAdapter implements PriorityOrdered {

	public static final String MOCKING_RESOURCE_ANNOTATION_PROCESSOR_BEAN_NAME = "MOCKING_RESOURCE_ANNOTATION_PROCESSOR_BEAN_NAME";

	private static ConfigurableListableBeanFactory beanFactory;

	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	//public static Map<Class, Class> aliasMap = new HashMap<Class, Class>();
	public static Map<String, String> aliasMap = new HashMap<String, String>();

	public MockResourceAnnotationBeanPostProcessor() {
		beanFactory.registerSingleton(MOCKING_RESOURCE_ANNOTATION_PROCESSOR_BEAN_NAME, this);
		System.out.println("Registered post processor as singleton for later retrieval: " +
				MOCKING_RESOURCE_ANNOTATION_PROCESSOR_BEAN_NAME);
	}

	public static void setBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		MockResourceAnnotationBeanPostProcessor.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		MockResources mockResources = AnnotationUtils.findAnnotation(
				bean.getClass(), MockResources.class);
		Object resultBean = bean;
		if (null != mockResources) {
			for (MockResource resource : mockResources.value()) {
				aliasMap.put(resource.beanName(), resource.mockBeanName());
				System.out.println("Added mocking alias for bean: " +
						resource.beanName() + " -> " +
						resource.mockBeanName());
			}
		}
		return resultBean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}
}
