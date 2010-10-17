package se.krite.springmock.context;

import java.lang.annotation.*;

/**
 * Annotation used to harvest unit test classes for spring bean override configurations
 * This annotation contains @MockResource annotations, that is the actual name override
 *
 * @author kristoffer.teuber
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
public @interface MockResources {
	MockResource[] value() default {};
}
