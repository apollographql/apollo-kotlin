package com.apollographql.apollo.gradle;

import com.apollographql.apollo.compiler.NullableValueType;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApolloExtension {
  static final String NAME = "apollo";
  private Map<String, String> customTypeMapping = new LinkedHashMap<>();
  private String nullableValueType = NullableValueType.ANNOTATED.getValue();
  private boolean useSemanticNaming = true;
  private boolean generateModelBuilder;
  private boolean useJavaBeansSemanticNaming = false;
  private boolean useRawTypesSuppression = false;
  private String schemaFilePath;
  private String outputPackageName;

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

  public void setUseSemanticNaming(boolean useSemanticNaming) {
    this.useSemanticNaming = useSemanticNaming;
  }

  public void setUseJavaBeansSemanticNaming(boolean useJavaBeansSemanticNaming) {
    this.useJavaBeansSemanticNaming = useJavaBeansSemanticNaming;
  }

  public void setUseRawTypesSuppression(boolean useRawTypesSuppression) {
    this.useRawTypesSuppression = useRawTypesSuppression;
  }

  public boolean isUseRawTypesSuppression() { return useRawTypesSuppression; }

  public boolean isUseSemanticNaming() {
    return useSemanticNaming;
  }

  public boolean isGenerateModelBuilder() {
    return generateModelBuilder;
  }

  public boolean isUseJavaBeansSemanticNaming() {
    return useJavaBeansSemanticNaming;
  }

  public void setGenerateModelBuilder(boolean generateModelBuilder) {
    this.generateModelBuilder = generateModelBuilder;
  }

  public void setCustomTypeMapping(Closure closure) {
    closure.setDelegate(customTypeMapping);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
  }

  public String getSchemaFilePath() {
    return schemaFilePath;
  }

  public void setSchemaFilePath(String schemaFilePath) {
    this.schemaFilePath = schemaFilePath;
  }

  public String getOutputPackageName() {
    return outputPackageName;
  }

  public void setOutputPackageName(String outputPackageName) {
    this.outputPackageName = outputPackageName;
  }
}
