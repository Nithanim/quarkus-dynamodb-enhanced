package org.acme.aws.dynamodb.fix.runtime;

import java.util.List;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.reflect.hosted.ReflectionFeature;

import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags;

@AutomaticFeature
public class DynamodbEnhancedFeatureFeature implements Feature {
  @Override
  public void afterCompilation(AfterCompilationAccess access) {}

  @Override
  public List<Class<? extends Feature>> getRequiredFeatures() {
    return List.of(ReflectionFeature.class);
  }

  @Override
  public void duringSetup(DuringSetupAccess access) {
    try {
      RuntimeReflection.register(DefaultAttributeConverterProvider.class.getConstructor());
      RuntimeReflection.register(BeanTableSchemaAttributeTags.class);
      RuntimeReflection.register(BeanTableSchemaAttributeTags.class.getMethods());
    } catch (NoSuchMethodException ex) {
      throw new IllegalStateException(
          "SVM Substitution: Unable to register method for reflection", ex);
    }
  }
}
