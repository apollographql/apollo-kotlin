package com.apollographql.ijplugin.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface ApolloProjectService {
  var isInitialized: Boolean
  val isApolloAndroid2Project: Boolean
  val isApolloKotlin3Project: Boolean
}

val Project.apolloProjectService get() = service<ApolloProjectService>()
