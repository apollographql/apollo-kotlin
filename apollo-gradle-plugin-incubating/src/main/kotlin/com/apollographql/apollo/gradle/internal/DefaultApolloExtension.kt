package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.Action
import org.gradle.api.Project

open class DefaultApolloExtension(project: Project): CompilerParams by DefaultCompilerParams(), ApolloExtension {
  /**
   * This is the input to the apollo plugin. Users will populate the services from their gradle files
   */
  val services = mutableListOf<DefaultService>()

  /**
   * compilationUnits is meant to be consumed by other gradle plugin.
   * The apollo plugin will add the {@link CompilationUnit} as it creates them
   */
  override val compilationUnits = project.container(CompilationUnit::class.java)

  override fun service(name: String, action: Action<DefaultService>) {
    val service = DefaultService(name)
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
  override fun setSchemaFilePath(schemaFilePath: String) {
    this.schemaFilePath = schemaFilePath
  }

  /**
   * For backward compatibility
   */
  override fun setOutputPackageName(outputPackageName: String) {
    this.outputPackageName = outputPackageName
  }
}