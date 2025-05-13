package com.apollographql.apollo.gradle.internal

import org.gradle.api.artifacts.ProjectDependency

internal fun ProjectDependency.getPathCompat(): String {
  val method = this::class.java.methods.firstOrNull {
    it.name == "getPath" && it.parameters.isEmpty()
  }
  return if (method != null) {
    // Gradle 8.11+ path
    // See https://docs.gradle.org/8.11/userguide/upgrading_version_8.html#deprecate_get_dependency_project
    method.invoke(this) as String
  } else {
    dependencyProject.path
  }
}
