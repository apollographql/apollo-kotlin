package com.apollographql.ijplugin.services

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface ApolloProjectService {
  // TODO keep this here, but put everything codegen related it a separate service
  val isApolloAndroid2Project: Boolean
  val isApolloKotlin3Project: Boolean

  // TODO use a TOPIC instead
  fun notifyGradleHasSynced()
}

fun Project.apolloProjectService() = service<ApolloProjectService>()
