package com.apollographql.apollo.gradle;

import com.apollographql.apollo.compiler.NullableValueType;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApolloExtension {
  static final String NAME = "apollo";
  private Map<String, String> customTypeMapping = new LinkedHashMap<>();
  private String nullableValueType = NullableValueType.ANNOTATED.getValue();
  private boolean generateAccessors = true;

  public Map<String, String> getCustomTypeMapping() {
    return customTypeMapping;
  }

  public void setCustomTypeMapping(Map<String, String> customTypeMapping) {
    this.customTypeMapping = customTypeMapping;
  }

  public String getNullableValueType() {
    return nullableValueType;
  }

  public void setNullableValueType(String nullableValueType) {
    this.nullableValueType = nullableValueType;
  }

  public boolean isGenerateAccessors() {
    return generateAccessors;
  }

  public void setGenerateAccessors(boolean generateAccessors) {
    this.generateAccessors = generateAccessors;
  }

  public void setCustomTypeMapping(Closure closure) {
    closure.setDelegate(customTypeMapping);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
  }
}
