package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.tooling.FieldInsights
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

@OptIn(ApolloExperimental::class)
class FieldInsightsService(private val project: Project) : Disposable {
  /**
   * Keys are the project name in the form "projectName/apolloServiceName", same format used
   * in [com.apollographql.ijplugin.graphql.GraphQLProjectFiles.name]
   */
  private val fieldLatenciesByProject = mutableMapOf<String, FieldInsights.FieldLatencies>()

  init {
    // TODO Random values for testing
    fieldLatenciesByProject["intellij-plugin-test-project/main"] = FieldInsights.FieldLatencies(
        listOf(
            FieldInsights.FieldLatencies.FieldLatency("Query", "animals", 42.0),
            FieldInsights.FieldLatencies.FieldLatency("Animal", "id", 0.5),
            FieldInsights.FieldLatencies.FieldLatency("Computer", "screen", 123.0),
        ),
    )
  }

  fun getLatency(graphQLProjectFilesName: String, typeName: String, fieldName: String): Double? {
    return fieldLatenciesByProject[graphQLProjectFilesName]?.getLatency(parentType = typeName, fieldName = fieldName)
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
