package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.compiler.OperationIdGenerator
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * CompilerParams contains all the parameters needed to invoke the apollo compiler.
 *
 * The setters are present for backward compatibility with kotlin build scripts and will go away
 * in a future release.
 */
interface CompilerParams {
  /**
   * Whether to generate java (default) or kotlin models
   */
  val generateKotlinModels: Property<Boolean>

  /**
   * Whether to generate the operation output json. The json contains information such as
   * operation id, name and complete source sent to the server. This can be useful if you need to upload
   * a query's exact content to a server that doesn't support automatic persisted queries.
   *
   * The operation output is written in [CompilationUnit.operationOutputFile]
   */
  val generateOperationOutput: Property<Boolean>


  /**
   * For custom scalar types like Date, map from the GraphQL type to the jvm/kotlin type.
   *
   * empty by default.
   */
  val customTypeMapping: MapProperty<String, String>

  /**
   * For custom persisted query Ids.
   *
   * If not provided, default hashing algorithm (sha256) will be used
   */
  val operationIdGenerator: Property<OperationIdGenerator>

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
   * By default, the plugin will use [Service.sourceFolder] to populate the graphqlSourceDirectorySet.
   *
   * You can override the default behaviour in either [ApolloExtension], [Service] or [CompilationUnit] by adding directories to graphqlSourceDirectorySet.
   * If you override the default behaviour, you're responsible of setting the includes and excludes accordingly. Typically, you would
   * set graphqlSourceDirectorySet.include("**&#47;*.graphql")
   *
   * Directories set on [ApolloExtension.graphqlSourceDirectorySet] or [Service.graphqlSourceDirectorySet] will not be used for test
   * variants as that would produce duplicate classes since the exact same files would be compiled for the main variants.
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
