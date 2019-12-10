package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.NullableValueType
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

class ApolloExtension {
  static final String NAME = "apollo"

  final Property<String> nullableValueType
  final Property<Boolean> useSemanticNaming
  final Property<Boolean> generateModelBuilder
  final Property<Boolean> useJavaBeansSemanticNaming
  final Property<Boolean> suppressRawTypesWarning
  final Property<Boolean> generateKotlinModels
  final Property<Boolean> generateVisitorForPolymorphicDatatypes
  final Property<String> schemaFilePath
  final Property<String> outputPackageName
  final Property<Boolean> generateTransformedQueries
  final MapProperty<String, String> customTypeMapping
  final Property<Boolean> generateAsInternal

  ApolloExtension(Project project) {
    nullableValueType = project.objects.property(String.class)
    nullableValueType.set(NullableValueType.ANNOTATED.getValue())

    useSemanticNaming = project.objects.property(Boolean.class)
    useSemanticNaming.set(true)

    generateModelBuilder = project.objects.property(Boolean.class)
    generateModelBuilder.set(false)

    useJavaBeansSemanticNaming = project.objects.property(Boolean.class)
    useJavaBeansSemanticNaming.set(false)

    suppressRawTypesWarning = project.objects.property(Boolean.class)
    suppressRawTypesWarning.set(false)

    generateKotlinModels = project.objects.property(Boolean.class)
    generateKotlinModels.set(false)

    generateVisitorForPolymorphicDatatypes = project.objects.property(Boolean.class)
    generateVisitorForPolymorphicDatatypes.set(false)

    schemaFilePath = project.objects.property(String.class)
    schemaFilePath.set("")

    outputPackageName = project.objects.property(String.class)
    outputPackageName.set("")

    generateTransformedQueries = project.objects.property(Boolean.class)
    generateTransformedQueries.set(false)

    customTypeMapping = project.objects.mapProperty(String.class, String.class)
    customTypeMapping.set(new LinkedHashMap())

    generateAsInternal = project.objects.property(Boolean.class)
    generateAsInternal.set(false)
  }

  void setNullableValueType(String nullableValueType) {
    this.nullableValueType.set(nullableValueType)
  }

  void setUseSemanticNaming(Boolean useSemanticNaming) {
    this.useSemanticNaming.set(useSemanticNaming)
  }

  void setGenerateModelBuilder(Boolean generateModelBuilder) {
    this.generateModelBuilder.set(generateModelBuilder)
  }

  void setUseJavaBeansSemanticNaming(Boolean useJavaBeansSemanticNaming) {
    this.useJavaBeansSemanticNaming.set(useJavaBeansSemanticNaming)
  }

  void setSuppressRawTypesWarning(Boolean suppressRawTypesWarning) {
    this.suppressRawTypesWarning.set(suppressRawTypesWarning)
  }

  void setGenerateKotlinModels(Boolean generateKotlinModels) {
    this.generateKotlinModels.set(generateKotlinModels)
  }

  void setGenerateVisitorForPolymorphicDatatypes(Boolean generateVisitorForPolymorphicDatatypes) {
    this.generateKotlinModels.set(generateVisitorForPolymorphicDatatypes)
  }

  void setSchemaFilePath(String schemaFilePath) {
    this.schemaFilePath.set(schemaFilePath)
  }

  void setOutputPackageName(String outputPackageName) {
    this.outputPackageName.set(outputPackageName)
  }

  void setGenerateTransformedQueries(Boolean generateTransformedQueries) {
    this.generateTransformedQueries.set(generateTransformedQueries)
  }

  void setGenerateOperationOutput(Boolean generateOperationOutput) {
    this.generateOperationOutput.set(generateOperationOutput)
  }

  void setGenerateAsInternal(Boolean generateAsInternal) {
    this.generateAsInternal.set(generateAsInternal)
  }

  void setCustomTypeMapping(Map customTypeMapping) {
    LinkedHashMap tmp = new LinkedHashMap()
    tmp.putAll(customTypeMapping)
    this.customTypeMapping.set(tmp)
  }
}
