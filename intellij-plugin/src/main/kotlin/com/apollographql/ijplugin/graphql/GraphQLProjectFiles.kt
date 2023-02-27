package com.apollographql.ijplugin.graphql

import com.intellij.util.messages.Topic

interface GraphQLProjectFilesListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<GraphQLProjectFilesListener> = Topic.create("GraphQLProjectFiles are available", GraphQLProjectFilesListener::class.java)
  }

  fun projectFilesAvailable()
}

data class GraphQLProjectFiles(
    val name: String,
    val schemaPaths: List<String>,
    val operationPaths: List<String>,
)
