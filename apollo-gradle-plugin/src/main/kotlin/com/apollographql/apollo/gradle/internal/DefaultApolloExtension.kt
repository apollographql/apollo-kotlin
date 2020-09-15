package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.Action
import org.gradle.api.Project

abstract class DefaultApolloExtension(val project: Project)
  : CompilerParams by project.objects.newInstance(DefaultCompilerParams::class.java)
    , ApolloExtension {
  /**
   * This is the input to the apollo plugin. Users will populate the services from their gradle files
   */
  val services = mutableListOf<DefaultService>()

  /**
   * compilationUnits is meant to be consumed by other gradle plugin.
   * The apollo plugin will add the {@link CompilationUnit} as it creates them
   */
  internal val compilationUnits = project.container(DefaultCompilationUnit::class.java)

  override fun onCompilationUnit(action: Action<CompilationUnit>) {
    compilationUnits.all(action)
  }

  override fun service(name: String, action: Action<DefaultService>) {
    val service = project.objects.newInstance(DefaultService::class.java, project.objects, name)
    action.execute(service)
    services.add(service)
  }
}
