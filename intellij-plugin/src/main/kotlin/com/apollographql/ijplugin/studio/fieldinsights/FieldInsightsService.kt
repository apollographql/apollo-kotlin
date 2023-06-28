package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.tooling.FieldInsights
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.util.logd
import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

@OptIn(ApolloExperimental::class)
class FieldInsightsService(private val project: Project) : Disposable {
  private val fieldLatenciesByService = mutableMapOf<ApolloKotlinService.Id, FieldInsights.FieldLatencies>()

  init {
    // TODO Random values for testing
    fieldLatenciesByService[ApolloKotlinService.Id("intellij-plugin-test-project", "main")] = FieldInsights.FieldLatencies(
        listOf(
            FieldInsights.FieldLatencies.FieldLatency("Query", "animals", 42.0),
            FieldInsights.FieldLatencies.FieldLatency("Animal", "id", 0.5),
            FieldInsights.FieldLatencies.FieldLatency("Computer", "screen", 123.0),
        ),
    )
  }

  fun getLatency(serviceId: ApolloKotlinService.Id, typeName: String, fieldName: String): Double? {
    return fieldLatenciesByService[serviceId]?.getLatency(parentType = typeName, fieldName = fieldName)
  }

  fun refreshHints() {
    @Suppress("UnstableApiUsage")
    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
    InlayHintsConfigurable.updateInlayHintsUI()
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
