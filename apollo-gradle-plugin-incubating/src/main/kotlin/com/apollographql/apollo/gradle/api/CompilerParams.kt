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

  /**
   * Whether to generate the transformed queries. Transformed queries are the queries as sent to the
   * server. This can be useful if you need to upload a query's exact content to a server that doesn't
   * support automatic persisted queries.
   *
   * The transformedQueries are written in [CompilationUnit.transformedQueriesDir]
   */
  val generateTransformedQueries: Property<Boolean>

  /**
   * Whether to generate the operation report. The report contains information such as
   * operation id, name and transformed query. This can be useful if you need to upload
   * a query's exact content to a server that doesn't support automatic persisted queries.
   *
   * The operation output is written in [CompilationUnit.operationOutputDir]
   */
  val generateOperationOutput: Property<Boolean>


  /**
   * For custom scalar types like Date, map from the GraphQL type to the jvm/kotlin type.
   *
   * empty by default.
   */
  val customTypeMapping: MapProperty<String, String>

  /**
   * The custom types code generate some warnings that might make the build fail.
   * suppressRawTypesWarning will add the appropriate SuppressWarning annotation
   *
   * false by default
   */
  val suppressRawTypesWarning: Property<Boolean>

  /**
   * Whether to suffix your queries, etc.. with `Query`, etc..
   *
   * true by default
   */
  val useSemanticNaming: Property<Boolean>

  /**
   * The nullable value type to use. One of: annotated, apolloOptional, guavaOptional, javaOptional, inputType
   *
   * annotated by default
   * only valid for java models as kotlin has nullable support
   */
  val nullableValueType: Property<String>

  /**
   * Whether to generate builders for java models
   *
   * false by default
   * only valid for java models as kotlin has data classes
   */
  val generateModelBuilder: Property<Boolean>

  /**
   * Whether to use java beans getters in the models.
   *
   * false by default
   * only valif for java as kotlin has properties
   */
  val useJavaBeansSemanticNaming: Property<Boolean>

  /**
   *
   */
  val generateVisitorForPolymorphicDatatypes: Property<Boolean>

  /**
   * The package name of the models is computed from their folder hierarchy like for java sources.
   *
   * If you want, you can prepend a custom package name here to namespace your models.
   *
   * The empty string by default.
   */
  val rootPackageName: Property<String>

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

  /**
   * Whether to generate Kotlin models with `internal` visibility modifier.
   *
   * false by default
   */
  val generateAsInternal: Property<Boolean>
}
