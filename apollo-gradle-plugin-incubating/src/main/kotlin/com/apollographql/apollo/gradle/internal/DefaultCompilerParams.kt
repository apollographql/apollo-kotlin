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
}
