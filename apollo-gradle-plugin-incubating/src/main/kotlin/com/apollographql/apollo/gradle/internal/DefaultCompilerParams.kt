package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class DefaultCompilerParams @Inject constructor(objects: ObjectFactory, val projectLayout: ProjectLayout) : CompilerParams {
  override val graphqlSourceDirectorySet = objects.sourceDirectorySet("graphql", "graphql")

  abstract override val schemaFile: RegularFileProperty
  override fun schemaFile(path: Any) {
    this.schemaFile.set { projectLayout.files(path).first() }
  }

  override val generateKotlinModels = objects.property(Boolean::class.java)
  override fun generateKotlinModels(generateKotlinModels: Boolean) {
    this.generateKotlinModels.set(generateKotlinModels)
  }

  override val generateTransformedQueries = objects.property(Boolean::class.java)
  override fun generateTransformedQueries(generateTransformedQueries: Boolean) {
    this.generateTransformedQueries.set(generateTransformedQueries)
  }

  override val customTypeMapping = objects.mapProperty(String::class.java, String::class.java)

  init {
    // see https://github.com/gradle/gradle/issues/7485
    customTypeMapping.set(null as Map<String, String>?)
  }

  override fun customTypeMapping(customTypeMapping: Map<String, String>) {
    this.customTypeMapping.set(customTypeMapping)
  }

  override val suppressRawTypesWarning = objects.property(Boolean::class.java)
  override fun suppressRawTypesWarning(suppressRawTypesWarning: Boolean) {
    this.suppressRawTypesWarning.set(suppressRawTypesWarning)
  }

  override val useSemanticNaming = objects.property(Boolean::class.java)
  override fun useSemanticNaming(useSemanticNaming: Boolean) {
    this.useSemanticNaming.set(useSemanticNaming)
  }

  override val nullableValueType = objects.property(String::class.java)
  override fun nullableValueType(nullableValueType: String) {
    this.nullableValueType.set(nullableValueType)
  }

  override val generateModelBuilder = objects.property(Boolean::class.java)
  override fun generateModelBuilder(generateModelBuilder: Boolean) {
    this.generateModelBuilder.set(generateModelBuilder)
  }

  override val useJavaBeansSemanticNaming = objects.property(Boolean::class.java)
  override fun useJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean) {
    this.useJavaBeansSemanticNaming.set(useJavaBeansSemanticNaming)
  }

  override val generateVisitorForPolymorphicDatatypes = objects.property(Boolean::class.java)
  override fun generateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean) {
    this.generateVisitorForPolymorphicDatatypes.set(generateVisitorForPolymorphicDatatypes)
  }

  override val rootPackageName = objects.property(String::class.java)
  override fun rootPackageName(rootPackageName: String) {
    this.rootPackageName.set(rootPackageName)
  }

  @Deprecated(message = "please use generateKotlinModels instead", replaceWith = ReplaceWith("generateKotlinModels"))
  override fun setGenerateKotlinModels(generateKotlinModels: Boolean) {
    System.err.println("setGenerateKotlinModels(Boolean) is deprecated, please use generateKotlinModels(Boolean) instead")
    generateKotlinModels(generateKotlinModels)
  }

  @Deprecated(message = "please use generateTransformedQueries instead", replaceWith = ReplaceWith("generateTransformedQueries"))
  override fun setGenerateTransformedQueries(generateTransformedQueries: Boolean) {
    System.err.println("setGenerateTransformedQueries(Boolean) is deprecated, please use generateTransformedQueries(Boolean) instead")
    generateTransformedQueries(generateTransformedQueries)
  }

  @Deprecated(message = "please use customTypeMapping instead", replaceWith = ReplaceWith("customTypeMapping"))
  override fun setCustomTypeMapping(customTypeMapping: Map<String, String>) {
    System.err.println("setCustomTypeMapping(Map<String,String>) is deprecated, please use customTypeMapping(Map<String,String>) instead")
    customTypeMapping(customTypeMapping)
  }

  @Deprecated(message = "please use suppressRawTypesWarning instead", replaceWith = ReplaceWith("suppressRawTypesWarning"))
  override fun setSuppressRawTypesWarning(suppressRawTypesWarning: Boolean) {
    System.err.println("setSuppressRawTypesWarning(Boolean) is deprecated, please use suppressRawTypesWarning(Boolean) instead")
    suppressRawTypesWarning(suppressRawTypesWarning)
  }

  @Deprecated(message = "please use useSemanticNaming instead", replaceWith = ReplaceWith("useSemanticNaming"))
  override fun setUseSemanticNaming(useSemanticNaming: Boolean) {
    System.err.println("setUseSemanticNaming(Boolean) is deprecated, please use useSemanticNaming(Boolean) instead")
    useSemanticNaming(useSemanticNaming)
  }

  @Deprecated(message = "please use nullableValueType instead", replaceWith = ReplaceWith("nullableValueType"))
  override fun setNullableValueType(nullableValueType: String) {
    System.err.println("setNullableValueType(String) is deprecated, please use nullableValueType(String) instead")
    nullableValueType(nullableValueType)
  }

  @Deprecated(message = "please use generateModelBuilder instead", replaceWith = ReplaceWith("generateModelBuilder"))
  override fun setGenerateModelBuilder(generateModelBuilder: Boolean) {
    System.err.println("setGenerateModelBuilder(Boolean) is deprecated, please use generateModelBuilder(Boolean) instead")
    generateModelBuilder(generateModelBuilder)
  }

  @Deprecated(message = "please use useJavaBeansSemanticNaming instead", replaceWith = ReplaceWith("useJavaBeansSemanticNaming"))
  override fun setUseJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean) {
    System.err.println("setUseJavaBeansSemanticNaming(Boolean) is deprecated, please use useJavaBeansSemanticNaming(Boolean) instead")
    useJavaBeansSemanticNaming(useJavaBeansSemanticNaming)
  }

  @Deprecated(message = "please use generateVisitorForPolymorphicDatatypes instead", replaceWith = ReplaceWith("generateVisitorForPolymorphicDatatypes"))
  override fun setGenerateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean) {
    System.err.println("setGenerateVisitorForPolymorphicDatatypes(Boolean) is deprecated, please use generateVisitorForPolymorphicDatatypes(Boolean) instead")
    generateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes)
  }
}
