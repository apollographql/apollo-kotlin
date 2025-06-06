package com.apollographql.ijplugin.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface ApolloProjectService {
  var isInitialized: Boolean

  enum class ApolloVersion {
    NONE,
    V2,
    V3,
    V4,
    V5,
    ;
    val isAtLeastV3 get() = this >= V3
    val isAtLeastV4 get() = this >= V4
    val isAtLeastV5 get() = this >= V5
  }

  val apolloVersion: ApolloVersion
}

val Project.apolloProjectService get() = service<ApolloProjectService>()
