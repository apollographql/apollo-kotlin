package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.NullableValueType
import org.gradle.api.Project
import org.gradle.api.provider.Property

class ApolloExtension {
  static final String NAME = "apollo"

  final Property<String> nullableValueType
  final Property<Boolean> useSemanticNaming
  final Property<Boolean> generateModelBuilder
  final Property<Boolean> useJavaBeansSemanticNaming
  final Property<Boolean> suppressRawTypesWarning
  final Property<String> schemaFilePath
  final Property<String> outputPackageName
  final Property<Map> customTypeMapping

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
}
