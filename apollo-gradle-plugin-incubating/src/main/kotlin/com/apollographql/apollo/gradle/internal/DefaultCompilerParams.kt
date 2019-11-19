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

  override val generateKotlinModels = objects.property(Boolean::class.java)

  override val generateTransformedQueries = objects.property(Boolean::class.java)

  override val customTypeMapping = objects.mapProperty(String::class.java, String::class.java)

  init {
    // see https://github.com/gradle/gradle/issues/7485
    customTypeMapping.set(null as Map<String, String>?)
  }

  override val suppressRawTypesWarning = objects.property(Boolean::class.java)

  override val useSemanticNaming = objects.property(Boolean::class.java)

  override val nullableValueType = objects.property(String::class.java)

  override val generateModelBuilder = objects.property(Boolean::class.java)

  override val useJavaBeansSemanticNaming = objects.property(Boolean::class.java)

  override val generateVisitorForPolymorphicDatatypes = objects.property(Boolean::class.java)

  override val rootPackageName = objects.property(String::class.java)
}
