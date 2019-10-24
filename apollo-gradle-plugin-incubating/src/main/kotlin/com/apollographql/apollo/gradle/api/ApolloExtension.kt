package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.gradle.internal.DefaultService
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

interface ApolloExtension: CompilerParams {
  /**
   * compilationUnits is meant to be consumed by other gradle plugin.
   * The apollo plugin will add the {@link CompilationUnit} as it creates them
   */
  val compilationUnits: NamedDomainObjectContainer<CompilationUnit>?

  fun service(name: String, action: Action<DefaultService>)

  fun setSchemaFilePath(schemaFilePath: String)

  fun setOutputPackageName(outputPackageName: String)
}