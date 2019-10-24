package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.Project

open class ApolloExtension(project: Project): CompilerParams by DefaultCompilerParams() {
  /**
   * This is the input to the apollo plugin. Users will populate the services from their gradle files
   */
  val services = mutableListOf<Service>()

  /**
   * compilationUnits is meant to be consumed by other gradle plugin.
   * The apollo plugin will add the {@link CompilationUnit} as it creates them
   */
  val compilationUnits = project.container(CompilationUnit::class.java)

  fun service(name: String, action: Action<Service>) {
    val service = Service(name)
    action.execute(service)
    services.add(service)
  }

  /**
   * Deprecated,use @{link service} instead
   */
  @JvmField
  var schemaFilePath: String? = null

  /**
   * Deprecated, use @{link service} instead
   */
  @JvmField
  var outputPackageName: String? = null

  /**
   * For backward compatibility
   */
  fun setSchemaFilePath(schemaFilePath: String) {
    this.schemaFilePath = schemaFilePath
  }

  /**
   * For backward compatibility
   */
  fun setOutputPackageName(outputPackageName: String) {
    this.outputPackageName = outputPackageName
  }
}