package com.apollographql.ijplugin.project

import com.intellij.util.messages.Topic

interface ApolloProjectListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<ApolloProjectListener> = Topic.create("Apollo project", ApolloProjectListener::class.java)
  }

  fun apolloProjectChanged(apolloVersion: ApolloProjectService.ApolloVersion)
}
