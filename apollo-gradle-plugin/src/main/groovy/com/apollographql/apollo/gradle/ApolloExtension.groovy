package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.NullableValueType
import org.gradle.api.Project
import org.gradle.api.provider.Property

class ApolloExtension {
  static final String NAME = "apollo"

  private final Property<String> nullableValueType
  private final Property<Boolean> useSemanticNaming
  private final Property<Boolean> generateModelBuilder
  private final Property<Boolean> useJavaBeansSemanticNaming
  private final Property<Boolean> suppressRawTypesWarning
  private final Property<String> schemaFilePath
  private final Property<String> outputPackageName
  private final Property<Map> customTypeMapping

  ApolloExtension(Project project) {
    nullableValueType = project.getObjects().property(String.class)
    nullableValueType.set(NullableValueType.ANNOTATED.getValue())

    useSemanticNaming = project.getObjects().property(Boolean.class)
    useSemanticNaming.set(true)

    generateModelBuilder = project.getObjects().property(Boolean.class)
    generateModelBuilder.set(false)

    useJavaBeansSemanticNaming = project.getObjects().property(Boolean.class)
    useJavaBeansSemanticNaming.set(false)

    suppressRawTypesWarning = project.getObjects().property(Boolean.class)
    suppressRawTypesWarning.set(false)

    schemaFilePath = project.getObjects().property(String.class)
    schemaFilePath.set("")

    outputPackageName = project.getObjects().property(String.class)
    outputPackageName.set("")

    customTypeMapping = project.getObjects().property(Map.class)
    customTypeMapping.set(new LinkedHashMap())
  }

  Property<String> getNullableValueType() {
    return nullableValueType
  }

  Property<Boolean> getUseSemanticNaming() {
    return useSemanticNaming
  }

  Property<Boolean> getGenerateModelBuilder() {
    return generateModelBuilder
  }

  Property<Boolean> getUseJavaBeansSemanticNaming() {
    return useJavaBeansSemanticNaming
  }

  Property<Boolean> getSuppressRawTypesWarning() {
    return suppressRawTypesWarning
  }

  Property<String> getSchemaFilePath() {
    return schemaFilePath
  }

  Property<String> getOutputPackageName() {
    return outputPackageName
  }

  Property<Map> getCustomTypeMapping() {
    return customTypeMapping
  }
}
