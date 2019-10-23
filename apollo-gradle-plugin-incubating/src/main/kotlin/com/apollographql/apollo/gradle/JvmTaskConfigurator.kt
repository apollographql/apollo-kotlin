package com.apollographql.apollo.gradle

import com.apollographql.apollo.gradle.api.CompilationUnit
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

object JvmTaskConfigurator {

  fun getVariants(project: Project): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)
    val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
    val sourceSets = javaPlugin.sourceSets

    // TODO: should we add tasks for the test sourceSet ?
    val name = "main"
    val apolloVariant = ApolloVariant(
        name = name,
        sourceSetNames = listOf(name),
        androidVariant = null
    )

    container.add(apolloVariant)
    return container
  }
}