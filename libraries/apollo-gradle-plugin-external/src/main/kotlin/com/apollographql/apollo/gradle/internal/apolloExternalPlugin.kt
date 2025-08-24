package com.apollographql.apollo.gradle.internal

import gratatouille.wiring.GPlugin
import org.gradle.api.Project

@Suppress("UNUSED_PARAMETER")
@GPlugin(id = "com.apollographql.apollo.external")
fun apolloExternalPlugin(project: Project) {
  error("The Apollo Gradle Plugin now uses classloader isolation and does not use R8 to relocate dependencies anymore. As a result, the `apollo-gradle-plugin-external` artifact and the `com.apollographql.apollo.external` plugins have been removed. You should use `apollo-gradle-plugin` and `com.apollographql.apollo` instead.")
}
