package se.krite.springmock.context;

import java.lang.annotation.*;

/**
 * Annotation used to harvest unit test classes for spring bean override configurations
 * This annotations is the actual name override, contained in the @MockResources annotation
 *
 * @author kristoffer.teuber
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
public @interface MockResource {
	String beanName();

	String mockBeanName() default "";

	boolean restoreToOriginal() default false;
}
