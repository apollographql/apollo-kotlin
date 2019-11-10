package com.apollographql.apollo.gradle.api

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * CompilerParams contains all the parameters needed to invoke the apollo compiler.
 *
 * The setters are present for backward compatibility with kotlin ubild scripts and will go away
 * in a future release.
 */
interface CompilerParams {
  /**
   * Whether to generate java (default) or kotlin models
   */
  val generateKotlinModels: Property<Boolean>
  fun generateKotlinModels(generateKotlinModels: Boolean)
  fun setGenerateKotlinModels(generateKotlinModels: Boolean)

  /**
   * Whether to generate the transformed queries. Transformed queries are the queries as sent to the
   * server. This can be useful if you need to upload a query's exact content to a server that doesn't
   * support automatic persisted queries.
   *
   * The transformedQueries are written in [CompilationUnit.transformedQueriesDir]
   */
  val generateTransformedQueries: Property<Boolean>
  fun generateTransformedQueries(generateTransformedQueries: Boolean)
  fun setGenerateTransformedQueries(generateTransformedQueries: Boolean)

  /**
   * For custom scalar types like Date, map from the GraphQL type to the jvm/kotlin type.
   *
   * empty by default.
   */
  val customTypeMapping: MapProperty<String, String>
  fun customTypeMapping(customTypeMapping: Map<String, String>)
  fun setCustomTypeMapping(customTypeMapping: Map<String, String>)

  /**
   * The custom types code generate some warnings that might make the build fail.
   * suppressRawTypesWarning will add the appropriate SuppressWarning annotation
   *
   * false by default
   */
  val suppressRawTypesWarning: Property<Boolean>
  fun suppressRawTypesWarning(suppressRawTypesWarning: Boolean)
  fun setSuppressRawTypesWarning(suppressRawTypesWarning: Boolean)

  /**
   * Whether to suffix your queries, etc.. with `Query`, etc..
   *
   * true by default
   */
  val useSemanticNaming: Property<Boolean>
  fun useSemanticNaming(useSemanticNaming: Boolean)
  fun setUseSemanticNaming(useSemanticNaming: Boolean)

  /**
   * The nullable value type to use. One of: annotated, apolloOptional, guavaOptional, javaOptional, inputType
   *
   * annotated by default
   * only valid for java models as kotlin has nullable support
   */
  val nullableValueType: Property<String>
  fun nullableValueType(nullableValueType: String)
  fun setNullableValueType(nullableValueType: String)

  /**
   * Whether to generate builders for java models
   *
   * false by default
   * only valid for java models as kotlin has data classes
   */
  val generateModelBuilder: Property<Boolean>
  fun generateModelBuilder(generateModelBuilder: Boolean)
  fun setGenerateModelBuilder(generateModelBuilder: Boolean)

  /**
   * Whether to use java beans getters in the models.
   *
   * false by default
   * only valif for java as kotlin has properties
   */
  val useJavaBeansSemanticNaming: Property<Boolean>
  fun useJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean)
  fun setUseJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean)

  /**
   *
   */
  val generateVisitorForPolymorphicDatatypes: Property<Boolean>
  fun generateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean)
  fun setGenerateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean)

  /**
   * The package name of the models is computed from their folder hierarchy like for java sources.
   *
   * If you want, you can prepend a custom package name here to namespace your models.
   *
   * The empty string by default.
   */
  val rootPackageName: Provider<String>
  fun rootPackageName(rootPackageName: String)

  /**
   * The graphql files containing the queries.
   *
   * This SourceDirectorySet includes .graphql and .gql files by default.
   *
   * By default, it will use [Service.sourceFolder] to populate the SourceDirectorySet.
   * You can override it from [ApolloExtension.onCompilationUnits] for more advanced use cases
   */
  val graphqlSourceDirectorySet: SourceDirectorySet

  /**
   * The schema file
   *
   * By default, it will use [Service.schemaFile] to set schemaFile.
   * You can override it from [ApolloExtension.onCompilationUnits] for more advanced use cases
   */
  val schemaFile: RegularFileProperty
  fun schemaFile(path: Any)
}
