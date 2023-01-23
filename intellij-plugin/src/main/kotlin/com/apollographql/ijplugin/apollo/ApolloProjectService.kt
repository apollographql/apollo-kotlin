package com.apollographql.ijplugin.apollo

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface ApolloProjectService {
  val isApolloAndroid2Project: Boolean
  val isApolloKotlin3Project: Boolean
}

val Project.apolloProjectService get() = service<ApolloProjectService>()
