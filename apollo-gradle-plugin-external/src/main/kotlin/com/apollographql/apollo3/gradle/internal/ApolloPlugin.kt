package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.ApolloExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

open class ApolloPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val defaultService = project.objects.newInstance(DefaultService::class.java, project, "service")
    project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project, defaultService) as DefaultApolloExtension
  }

  companion object {
    internal val extraHeaders = mapOf(
        "apollographql-client-name" to "apollo-gradle-plugin",
        "apollographql-client-version" to com.apollographql.apollo3.compiler.APOLLO_VERSION
    )
  }
}
