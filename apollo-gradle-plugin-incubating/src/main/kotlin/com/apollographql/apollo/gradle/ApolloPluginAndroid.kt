package com.apollographql.apollo.gradle

import org.gradle.api.Project

open class ApolloPluginAndroid : ApolloPlugin() {
  override fun apply(project: Project) {
    project.logger.error("The `com.apollographql.android` plugin works for non-android as well. You can use `com.apollographql.apollo` if you prefer.")
    super.apply(project)
  }
}