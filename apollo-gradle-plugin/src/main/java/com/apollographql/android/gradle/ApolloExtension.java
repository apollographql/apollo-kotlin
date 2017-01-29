package com.apollographql.android.gradle;

import java.util.HashMap;
import java.util.Map;

public class ApolloExtension {
  static final String NAME = "apollo";
  private boolean generateClasses = false;
  private Map<String, String> customTypeMapping = new HashMap<>();

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
}
