package com.apollographql.ijplugin.services

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface ApolloProjectService {
  val isApolloAndroid2Project: Boolean
  val isApolloKotlin3Project: Boolean
}

fun Project.apolloProjectService() = service<ApolloProjectService>()
