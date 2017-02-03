package com.apollographql.android.gradle;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApolloExtension {
  static final String NAME = "apollo";
  private boolean generateClasses = true;
  private Map<String, String> customTypeMapping = new LinkedHashMap<>();

  public boolean isGenerateClasses() {
    return generateClasses;
  }

  public void setGenerateClasses(boolean generateClasses) {
    this.generateClasses = generateClasses;
  }

  public Map<String, String> getCustomTypeMapping() {
    return customTypeMapping;
  }

  public void setCustomTypeMapping(Map<String, String> customTypeMapping) {
    this.customTypeMapping = customTypeMapping;
  }

  public void setCustomTypeMapping(Closure closure) {
    closure.setDelegate(customTypeMapping);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
  }
}
