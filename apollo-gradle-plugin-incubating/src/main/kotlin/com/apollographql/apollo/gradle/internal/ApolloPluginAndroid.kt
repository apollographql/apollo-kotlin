package com.apollographql.apollo.gradle.internal

import org.gradle.api.Project

open class ApolloPluginAndroid : ApolloPlugin() {
  override fun apply(project: Project) {
    project.logger.error("The `com.apollographql.android` plugin is deprecated. Please use `com.apollographql.apollo` instead.")
    super.apply(project)
  }
}