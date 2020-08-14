package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class DefaultCompilerParams @Inject constructor(objects: ObjectFactory) : CompilerParams {
  override val graphqlSourceDirectorySet = objects.sourceDirectorySet("graphql", "graphql")

  abstract override val schemaFile: RegularFileProperty

  abstract override val generateKotlinModels : Property<Boolean>

  abstract override val customTypeMapping: MapProperty<String, String>

  abstract override val operationIdGenerator: Property<OperationIdGenerator>

  abstract override val suppressRawTypesWarning : Property<Boolean>

  abstract override val useSemanticNaming : Property<Boolean>

  abstract override val nullableValueType : Property<String>

  abstract override val generateModelBuilder : Property<Boolean>

  abstract override val useJavaBeansSemanticNaming : Property<Boolean>

  abstract override val generateVisitorForPolymorphicDatatypes : Property<Boolean>

  abstract override val rootPackageName : Property<String>

  abstract override val generateAsInternal: Property<Boolean>

  abstract override val sealedClassesForEnumsMatching: ListProperty<String>

  abstract override val alwaysGenerateTypesMatching: SetProperty<String>

  init {
    // see https://github.com/gradle/gradle/issues/7485
    customTypeMapping.convention(null as Map<String, String>?)
    sealedClassesForEnumsMatching.convention(null as List<String>?)
    alwaysGenerateTypesMatching.convention(null as Set<String>?)
  }

}
