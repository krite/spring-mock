package se.krite.springmock.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SimpleBeanTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Mocking implementation of spring's bean factory.
 * It is used for making injection of mocking implementations easier.
 * The factory will look in it's alias list before resolving a dependency, thus making it
 * possible to override the name of an injected resource.
 * The population of the alias list is done using a custom annotation.
 * If an alias is found, a jdk proxy is created.
 * This proxy is later used by MockResourceTestExecutionListener to redirect to correct implementation
 * when needed
 *
 * @author kristoffer.teuber
 */
public class MockingAliasingCglibSubclassingInstantiationStrategy extends CglibSubclassingInstantiationStrategy {

	private static final Logger log = LoggerFactory.getLogger(
			MockingAliasingCglibSubclassingInstantiationStrategy.class);

	public static Object createProxy(Class beanClass, String targetBeanName,
									 Class mockingClass, BeanFactory beanFactory) {
		SimpleBeanTargetSource targetSource = createTargetSource(targetBeanName, mockingClass, beanFactory);
		ProxyFactory proxyFactory = new ProxyFactory();
		ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

		Class[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, proxyClassLoader);
		for (Class targetInterface : targetInterfaces) {
			proxyFactory.addInterface(targetInterface);
		}
		proxyFactory.setTargetSource(targetSource);
		return proxyFactory.getProxy(proxyClassLoader);
	}

	public static SimpleBeanTargetSource createTargetSource(
			String targetBeanName, Class mockingClass, BeanFactory beanFactory) {
		SimpleBeanTargetSource targetSource = new SimpleBeanTargetSource();
		targetSource.setTargetBeanName(targetBeanName);
		targetSource.setTargetClass(mockingClass);
		targetSource.setBeanFactory(beanFactory);
		return targetSource;
	}

	private void modifyProxy(BeanFactory owner, Object object,
							 BeanDefinition beanDefinition, String mockBeanName) {
		// Check if we have a proxy already in the cache
		if (object instanceof Advised) {
			Advised advised = (Advised) object;
			// Find and set a new target source
			SimpleBeanTargetSource newTargetSource = MockingAliasingCglibSubclassingInstantiationStrategy.
					createTargetSource(mockBeanName, beanDefinition.getClass(), owner);
			advised.setTargetSource(newTargetSource);
		}
	}

	private Object getProxyObject(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
								  Constructor ctor, Object factoryBean, Method factoryMethod, Object[] args) {

		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) owner;
		MockingClassLoader.setBeanFactory(beanFactory);

		Class beanClass = beanDefinition.getBeanClass();
		Object proxy;
		String actualTargetBeanName = beanName + "_actual";

		// If we should be mocked, create one more proxy on top of old proxy
		String mockBeanName = MockingClassLoader.peekMockNameFromStack(beanName);
		if (mockBeanName != null) {
			// Create a proxy with the original name
			log.debug("Found mock configuration for bean: " + beanName + " -> " + mockBeanName +
					"(" + beanDefinition.getBeanClassName() + ")");
			proxy = createProxy(beanClass, actualTargetBeanName, beanClass, owner);
		} else {
			log.debug("Instantiating bean: " + beanName + " -> " + beanDefinition.getBeanClassName());
			proxy = this.instantiate(beanDefinition, beanName, owner,
					ctor, factoryBean, factoryMethod, args);
		}
		return proxy;
	}

	private Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner, Constructor ctor,
							   Object factoryBean, Method factoryMethod, Object[] args) {
		if (ctor != null) {
			return super.instantiate(beanDefinition, beanName, owner, ctor, args);
		} else if (factoryBean != null && factoryMethod != null) {
			return super.instantiate(beanDefinition, beanName, owner, factoryBean, factoryMethod, args);
		} else {
			return super.instantiate(beanDefinition, beanName, owner);
		}
	}

	@Override
	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
		return this.getProxyObject(beanDefinition, beanName, owner, null, null, null, null);
	}

	@Override
	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
							  Constructor ctor, Object[] args) {
		return this.getProxyObject(beanDefinition, beanName, owner, ctor, null, null, args);
	}

	@Override
	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
							  Object factoryBean, Method factoryMethod, Object[] args) {
		return this.getProxyObject(beanDefinition, beanName, owner, null, factoryBean, factoryMethod, args);
	}
}
