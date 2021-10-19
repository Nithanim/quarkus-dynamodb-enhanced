package me.nithanim.aws.dynamodb.enhanced.nativeimage.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dynamodb-enhanced", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class BuildTimeConfig {

  /** Apply patch for crashing tests. Reduces performance in JVM mode. */
  @ConfigItem(defaultValue = "true")
  public boolean jvmTransformation;
}
