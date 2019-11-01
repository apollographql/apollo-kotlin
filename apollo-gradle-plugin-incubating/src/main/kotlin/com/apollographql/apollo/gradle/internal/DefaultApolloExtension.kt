package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property

open class DefaultApolloExtension(val project: Project): CompilerParams by DefaultCompilerParams(project.objects), ApolloExtension {
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
    val service = project.objects.newInstance(DefaultService::class.java, project.objects, name)
    action.execute(service)
    services.add(service)
  }

  override val outputPackageName = project.objects.property(String::class.java)
  override val schemaFilePath = project.objects.property(String::class.java)

  /**
   * For backward compatibility
   */
  override fun setSchemaFilePath(schemaFilePath: String) {
    this.schemaFilePath.set(schemaFilePath)
  }

  /**
   * For backward compatibility
   */
  override fun setOutputPackageName(outputPackageName: String) {
    this.outputPackageName.set(outputPackageName)
  }
}