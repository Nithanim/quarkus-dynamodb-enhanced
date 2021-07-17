package org.acme.aws.dynamodb.fix.runtime;

import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class holds the new method bodies (implementation) of the methods that are replaced by
 * substitution in native-image.
 */
public class BeanTableSchemaSubstitutionImplementation {
  public static <T> Supplier<T> newObjectSupplierForClass(Class<T> clazz) {
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflectConstructor(clazz.getConstructor());
      return new ConstructorWrapper<>(mh);
    } catch (IllegalAccessException | NoSuchMethodException ex) {
      throw new IllegalStateException(
          "SVM Substitution: Unable to convert Constructor to MethodHandle!", ex);
    }
  }

  public static <T, R> Function<T, R> getterForProperty(
      PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
    Method readMethod = propertyDescriptor.getReadMethod();
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflect(readMethod);
      return new GetterWrapper<>(mh);
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(
          "SVM Substitution: Unable to convert Getter-Method to MethodHandle!", ex);
    }
  }

  public static <T, U> BiConsumer<T, U> setterForProperty(
      PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
    Method writeMethod = propertyDescriptor.getWriteMethod();
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflect(writeMethod);
      return new SetterWrapper<>(mh);
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(
          "SVM Substitution: Unable to convert Setter-Method to MethodHandle!", ex);
    }
  }

  private static class ConstructorWrapper<T> implements Supplier<T> {
    private final MethodHandle mh;

    public ConstructorWrapper(MethodHandle mh) {
      this.mh = mh;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
      try {
        return (T) mh.invoke();
      } catch (Exception ex) {
        throw new IllegalStateException("SVM Substitution: Exception invoking getter!", ex);
      } catch (Error error) {
        throw error;
      } catch (Throwable throwable) {
        throw new Error(
            "SVM Substitution: No other direct descendant of Throwable should exist!", throwable);
      }
    }
  }

  private static class GetterWrapper<T, R> implements Function<T, R> {
    private final MethodHandle mh;

    public GetterWrapper(MethodHandle mh) {
      this.mh = mh;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R apply(T t) {
      try {
        return (R) mh.invoke(t);
      } catch (Exception ex) {
        throw new IllegalStateException("SVM Substitution: Exception invoking getter!", ex);
      } catch (Error error) {
        throw error;
      } catch (Throwable throwable) {
        throw new Error(
            "SVM Substitution: No other direct descendant of Throwable should exist!", throwable);
      }
    }
  }

  private static class SetterWrapper<T, U> implements BiConsumer<T, U> {
    private final MethodHandle mh;

    public SetterWrapper(MethodHandle mh) {
      this.mh = mh;
    }

    @Override
    public void accept(T object, U value) {
      try {
        mh.invoke(object, value);
      } catch (Exception ex) {
        throw new IllegalStateException("SVM Substitution: Exception invoking getter!", ex);
      } catch (Error error) {
        throw error;
      } catch (Throwable throwable) {
        throw new Error(
            "SVM Substitution: No other direct descendant of Throwable should exist!", throwable);
      }
    }
  }
}
