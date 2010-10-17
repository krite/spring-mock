package se.krite.springmock.context;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.target.SimpleBeanTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * A utility class that harvests all classes and their annotations from a given folder.
 * It holds the actual annotation configuration for the mocking framework.
 *
 * @author kristoffer.teuber
 */
public class MockingClassLoader {

	// Base package to use for test classes. All *Test.class under this
	// package will be scanned and mocked
	private static final String basePackage = "se.krite.springmock";
	private static final Logger log = LoggerFactory.getLogger(MockingClassLoader.class);
	private static final String BEAN_NAME_RESTORE_TO_DEFAULT = "_mock_resource_restore_bean_to_default_";

	private static volatile boolean classesInitialized = false;
	private static Map<String, Set<Class<?>>> beanNameMockedAtClassLevel = new HashMap<String, Set<Class<?>>>();
	private static final Map<String, Stack<String>> aliasMap = new HashMap<String, Stack<String>>();
	private static DefaultListableBeanFactory beanFactory;

	private static String getBasePathFromObjectInstanceByNamedPackage(Object object, String breakAtPackage) {
		Class cls = object.getClass();
		String className = cls.getName().concat(".class");
		Package pck = cls.getPackage();
		String packageName;
		if (pck != null) {
			packageName = pck.getName();
			if (className.startsWith(packageName)) {
				className = className.substring(packageName.length() + 1);
			}
		}
		URL url = cls.getResource(className);
		String classFilePath = url.getPath().substring(1);
		classFilePath = classFilePath.replaceAll("%20", " ");
		return StringUtils.substringBefore(classFilePath, breakAtPackage.replaceAll("\\.", "/"));
	}

	private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (packageName != null && packageName.trim().length() > 0 && !packageName.endsWith(".")) {
				packageName += ".";
			}
			if (file.isDirectory()) {
				classes.addAll(findClasses(file, packageName + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				String fileName = StringUtils.substringBefore(file.getName(), ".class");
				if (fileName.endsWith("Test")) {
					String className = packageName + StringUtils.substringBefore(file.getName(), ".class");
					log.debug("Found test class to load: " + className);
					classes.add(Class.forName(className));
				}
			}
		}
		return classes;
	}

	private static Class[] getAllClassesFromObjectInstanceByNamedPackage(Object object, String breakAtPackage) {
		String basePath = getBasePathFromObjectInstanceByNamedPackage(object, breakAtPackage);
		log.debug("Starting to scan test classes from base path: " + basePath);
		try {
			List<Class> classes = findClasses(new File(basePath), "");
			return classes.toArray(new Class[classes.size()]);
		} catch (Throwable t) {
			log.error("Could not load classes!", t);
		}
		return null;
	}

	private static void loadClassesByInstance(Object testClassObject) {
		log.debug("Finding test classes based on class: " + testClassObject.getClass().getName() +
				" and base package: " + basePackage);
		Class[] classes = getAllClassesFromObjectInstanceByNamedPackage(testClassObject, basePackage);
		if (classes == null) {
			log.error("Could not load classes, se previous error...");
			return;
		}
		for (Class clazz : classes) {
			List<MockResource> resources = new ArrayList<MockResource>();
			MockResource mockResource = AnnotationUtils.findAnnotation(clazz, MockResource.class);
			if (mockResource != null) {
				resources.add(mockResource);
			}
			MockResources mockResources = AnnotationUtils.findAnnotation(clazz, MockResources.class);
			if (mockResources != null && mockResources.value() != null && mockResources.value().length > 0) {
				resources.addAll(Arrays.asList(mockResources.value()));
			}

			// All beans that has a alias defined at class level, will not be reset to its original impl
			for (MockResource resetResource : resources) {
				addClassToClassLevelMockingForBean(resetResource.beanName(), clazz);
			}

			// At class level, add bean to mocking map for our instantiation strategy to find,
			// or we will not get a correct proxy
			for (Method initMethod : ReflectionUtils.getAllDeclaredMethods(clazz)) {
				if (initMethod != null) {
					mockResources = AnnotationUtils.findAnnotation(
							initMethod, MockResources.class);
					mockResource = AnnotationUtils.findAnnotation(
							initMethod, MockResource.class);
					if (mockResource != null) {
						resources.add(mockResource);
					}
					if (mockResources != null && mockResources.value() != null && mockResources.value().length > 0) {
						resources.addAll(Arrays.asList(mockResources.value()));
					}
				}
			}

			// Initialize all stacks with original instance
			for (MockResource initResource : resources) {
				pushMockBeanNameOntoStack(initResource.beanName(), null);
			}
		}
	}

	public static void registerAliases(TestContext testContext) throws Exception {
		synchronized (aliasMap) {
			if (classesInitialized) {
				log.debug("Classes already loaded...");
				return;
			}
			Object bean = testContext.getTestInstance();
			loadClassesByInstance(bean);
			classesInitialized = true;
		}
	}

	public static void modifyMockingProxies(TestContext testContext) {
		Object testClassObject = testContext.getTestInstance();
		Method method = testContext.getTestMethod();

		List<MockResource> resources = new ArrayList<MockResource>();
		MockResources mockResources;
		if (method != null) {
			MockResource resource;
			mockResources = AnnotationUtils.findAnnotation(
					method, MockResources.class);
			resource = AnnotationUtils.findAnnotation(
					method, MockResource.class);
			if (resource != null) {
				resources.add(resource);
			}
			if (mockResources != null && mockResources.value() != null && mockResources.value().length > 0) {
				resources.addAll(Arrays.asList(mockResources.value()));
			}

		} else if (testClassObject != null) {
			Class clazz = testClassObject.getClass();
			MockResource resource;
			mockResources = AnnotationUtils.findAnnotation(clazz, MockResources.class);
			resource = AnnotationUtils.findAnnotation(clazz, MockResource.class);
			if (resource != null) {
				resources.add(resource);
			}
			if (mockResources != null && mockResources.value() != null && mockResources.value().length > 0) {
				resources.addAll(Arrays.asList(mockResources.value()));
			}

			/*
			// All beans that has a alias defined at class level, will not be reset to its original impl
			for (MockResource resetResource : resources) {
				beanNameMockedAtClassLevel.put(resetResource.beanName(), testClassObject.getClass());
			}
			*/

			// At class level, add bean to mocking map for our instantiation strategy to find,
			// or we will not get a correct proxy
			for (Method initMethod : ReflectionUtils.getAllDeclaredMethods(clazz)) {
				List<MockResource> initResources = new ArrayList<MockResource>();
				if (initMethod != null) {
					mockResources = AnnotationUtils.findAnnotation(
							initMethod, MockResources.class);
					resource = AnnotationUtils.findAnnotation(
							initMethod, MockResource.class);
					if (resource != null) {
						initResources.add(resource);
					}
					if (mockResources != null && mockResources.value() != null && mockResources.value().length > 0) {
						initResources.addAll(Arrays.asList(mockResources.value()));
					}
					for (MockResource initResource : initResources) {
						pushMockBeanNameOntoStack(initResource.beanName(), null);
					}
				}
			}
		}


		for (MockResource resource : resources) {
			// Fill up map of aliases
			// Override if it exists, create otherwise
			String beanName = resource.beanName();
			String mockBeanName;
			if (resource.restoreToOriginal()) {
				mockBeanName = beanName + "_actual";
			} else {
				mockBeanName = resource.mockBeanName();
			}
			pushMockBeanNameOntoStack(resource.beanName(), mockBeanName);

			if (beanFactory == null) {
				continue;
			}
			if (beanName.equals(mockBeanName)) {
				continue;
			}
			modifyProxy(beanName, mockBeanName);
		}
	}

	public static void modifyProxy(String beanName, String mockBeanName) {
		// Check if we have a proxy already in the cache
		Object object = beanFactory.getBean(beanName);
		if (object instanceof Advised) {
			Advised advised = (Advised) object;
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(mockBeanName);
			SimpleBeanTargetSource newTargetSource = MockingAliasingCglibSubclassingInstantiationStrategy.
					createTargetSource(mockBeanName, beanDefinition.getClass(), beanFactory);
			advised.setTargetSource(newTargetSource);
		}
	}

	private static void addClassToClassLevelMockingForBean(String beanName, Class<?> clazz) {
		Set<Class<?>> classes = beanNameMockedAtClassLevel.get(beanName);
		if (classes == null) {
			classes = new HashSet<Class<?>>();
			beanNameMockedAtClassLevel.put(beanName, classes);
		}
		classes.add(clazz);
	}

	public static void pushMockBeanNameOntoStack(String beanName, String mockBeanName) {
		Stack<String> stack = aliasMap.get(beanName);
		if (stack == null) {
			stack = new Stack<String>();
			aliasMap.put(beanName, stack);
			// Push original to stack first
			stack.push(beanName + "_actual");
			log.debug("Initialized alias for bean: " + beanName);
		}
		if (mockBeanName != null) {
			stack.push(mockBeanName);
			log.debug("Pushed mocking alias for bean: " + beanName + " -> " + mockBeanName);
		}
	}

	public static void resetAllProxiesToDefaultValues(TestContext testContext) {
		if (beanFactory == null) {
			return;
		}
		Class<?> testClass = testContext.getTestClass();
		log.debug("Resetting all proxies to default mocking values (if any)...");
		for (String beanName : aliasMap.keySet()) {
			Stack<String> stack = aliasMap.get(beanName);
			if (stack != null) {
				// Pop stack of impl down to original or mocked impl depending upon
				// class level annotation
				int popToLimit = 2;
				Set<Class<?>> beanMockedForClasses = beanNameMockedAtClassLevel.get(beanName);
				if (beanMockedForClasses == null || !beanMockedForClasses.contains(testClass)) {
					popToLimit = 1;
				}
				while (stack.size() > popToLimit) {
					stack.pop();
				}
				String mockBeanName = stack.peek();
				if (mockBeanName != null) {
					log.debug("Resetting bean: " + beanName + " -> " + mockBeanName);
					modifyProxy(beanName, mockBeanName);
				}
			}
		}
		log.debug("Done resetting proxies!");
	}

	// Public static accessors

	public static String peekMockNameFromStack(String beanName) {
		Stack<String> stack = aliasMap.get(beanName);
		if (stack == null) {
			return null;
		}
		String mockBeanName = stack.peek();
		log.debug("Peeked mocking alias for bean: " + beanName + " -> " + mockBeanName);
		return mockBeanName;
	}

	public static List<BeanDefinitionHolder> getActualImplementationBeans(ConfigurableListableBeanFactory beanFactory) {
		List<BeanDefinitionHolder> holders = new ArrayList<BeanDefinitionHolder>();
		for (String beanName : aliasMap.keySet()) {
			Stack<String> stack = aliasMap.get(beanName);
			if (stack != null) {
				BeanDefinition original = beanFactory.getBeanDefinition(beanName);
				if (original != null) {
					holders.add(new BeanDefinitionHolder(original, stack.firstElement()));
				}
			}
		}
		return holders;
	}

	public static void setBeanFactory(DefaultListableBeanFactory factory) {
		beanFactory = factory;
	}
}
