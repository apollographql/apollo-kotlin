package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.Action
import org.gradle.api.Project

open class DefaultApolloExtension(val project: Project)
  : CompilerParams by project.objects.newInstance(DefaultCompilerParams::class.java)
    , ApolloExtension {
  /**
   * This is the input to the apollo plugin. Users will populate the services from their gradle files
   */
  val services = project.objects.domainObjectContainer(DefaultService::class.java)

  /**
   * compilationUnits is meant to be consumed by other gradle plugins.
   * The apollo plugin will add the {@link CompilationUnit} as it creates them
   */
  internal val compilationUnits = project.container(CompilationUnit::class.java)

  override fun onCompilationUnits(action: Action<CompilationUnit>) {
    compilationUnits.all(action)
  }

  override fun service(name: String, action: Action<DefaultService>) {
    val service = project.objects.newInstance(DefaultService::class.java, project.objects, name)
    action.execute(service)
    if (!services.add(service)) {
      if (name == "service") {
        throw IllegalArgumentException("\"service\" is a reserved service name, please use something else.")
      }
      throw IllegalArgumentException("a service with name \"$name\" was already registered, please use something else")
    }
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
