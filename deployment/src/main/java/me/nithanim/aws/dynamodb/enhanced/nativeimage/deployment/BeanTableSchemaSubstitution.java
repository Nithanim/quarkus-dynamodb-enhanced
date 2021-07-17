package me.nithanim.aws.dynamodb.enhanced.nativeimage.deployment;

import java.beans.PropertyDescriptor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import me.nithanim.aws.dynamodb.enhanced.nativeimage.runtime.BeanTableSchemaSubstitutionImplementation;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;

/**
 * Although method handles are now supported by GraalVM
 * (https://github.com/oracle/graal/issues/2761), the {@link ObjectConstructor} (and others)
 * dynamically creates an efficient suppliers (lambda) for the passed methods.
 *
 * <p>In static, normal java code, this would look like HashMap::new. Since this has to be done with
 * any (unknown) classes and creating via reflection is slow, this is a workaround to create this
 * "static" code dynamically at runtime for maximum performance.
 *
 * <p>The problem now is, that this "lambda" is effectively a new class. Since in the native image
 * the compilation is already done, it is not possible to add additional code at runtime which this
 * new class is.
 *
 * <p>The solution is, not to create lambdas at runtime but rather to stick to ordinary method
 * handles. With them we can efficiently create new objects of classes but we still need the wrapper
 * (supplier, function ,...) for them. The workaround for that is to use the good old inner classes.
 */
@TargetClass(BeanTableSchema.class)
public final class BeanTableSchemaSubstitution {

  @Substitute
  private static <T> Supplier<T> newObjectSupplierForClass(Class<T> clazz) {
    return BeanTableSchemaSubstitutionImplementation.newObjectSupplierForClass(clazz);
  }

  @Substitute
  private static <T, R> Function<T, R> getterForProperty(
      PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
    return BeanTableSchemaSubstitutionImplementation.getterForProperty(propertyDescriptor, beanClass);
  }

  @Substitute
  private static <T, U> BiConsumer<T, U> setterForProperty(
      PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
    return BeanTableSchemaSubstitutionImplementation.setterForProperty(propertyDescriptor, beanClass);
  }
}
