package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.tooling.FieldInsights
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.gradle.ApolloKotlinServiceListener
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.settings.ApolloKotlinServiceConfiguration
import com.apollographql.ijplugin.settings.SettingsListener
import com.apollographql.ijplugin.settings.SettingsState
import com.apollographql.ijplugin.settings.settingsState
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val FETCH_PERIOD_HOURS = 12L

@OptIn(ApolloExperimental::class)
class FieldInsightsService(private val project: Project) : Disposable {
  /**
   * Keys are the project name in the form "projectName/apolloServiceName", same format used
   * in [com.apollographql.ijplugin.graphql.GraphQLProjectFiles.name]
   */
  private var fieldLatenciesByService = mapOf<String, FieldInsights.FieldLatencies>()
  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val executor : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var fetchLatenciesFuture: ScheduledFuture<*>? = null

  init {
    logd("project=${project.name}")
    startObservingSettings()
    startObservingGraphQLProjectFiles()
  }

  private fun startObservingSettings() {
    logd()
    project.messageBus.connect(this).subscribe(SettingsListener.TOPIC, object : SettingsListener {
      var serviceConfigurations: List<ApolloKotlinServiceConfiguration> = project.settingsState.apolloKotlinServiceConfigurations
      override fun settingsChanged(settingsState: SettingsState) {
        val serviceConfigurationChanged = serviceConfigurations != settingsState.apolloKotlinServiceConfigurations
        serviceConfigurations = settingsState.apolloKotlinServiceConfigurations
        logd("serviceConfigurationChanged=$serviceConfigurationChanged")
        if (serviceConfigurationChanged) {
          scheduleFetchLatencies()
        }
      }
    })
  }

  private fun startObservingGraphQLProjectFiles() {
    project.messageBus.connect(this).subscribe(ApolloKotlinServiceListener.TOPIC, object : ApolloKotlinServiceListener {
      override fun apolloKotlinServicesAvailable() {
        logd()
        scheduleFetchLatencies()
      }
    })
  }

  private fun scheduleFetchLatencies() {
    fetchLatenciesFuture?.cancel(false)
    fetchLatenciesFuture = executor.scheduleAtFixedRate(::fetchLatencies, 0, FETCH_PERIOD_HOURS, TimeUnit.HOURS)
  }

  private fun fetchLatencies() {
    logd()
    val graphQLProjectFiles = GradleToolingModelService.getApolloKotlinServices(project)
    val graphQLProjectFilesToApiKey: Map<ApolloKotlinService, ApolloKotlinServiceConfiguration> = graphQLProjectFiles.associateWith { gqlProject ->
      project.settingsState.apolloKotlinServiceConfigurations.firstOrNull { serviceConfiguration ->
        serviceConfiguration.id == gqlProject.id.toString()
      }
    }.filterValues { it != null }.mapValues { it.value!! }
    val deferredLatenciesByProject = graphQLProjectFilesToApiKey.mapNotNull { (gqlProject, serviceConfiguration) ->
      serviceConfiguration.graphOsApiKey?.let { apiKey ->
        gqlProject.id.toString() to coroutineScope.async {
          FieldInsights.fetchFieldLatencies(
              apiKey = apiKey,
              serviceId = serviceConfiguration.graphOsGraphName,
          )
        }
      }
    }
    coroutineScope.launch {
      val fieldLatenciesByProject = mutableMapOf<String, FieldInsights.FieldLatencies>()
      for ((projectName, deferred) in deferredLatenciesByProject) {
        val result = try {
          deferred.await()
        } catch (e: Exception) {
          FieldInsights.FieldLatenciesResult.Error(e)
        }
        when (result) {
          is FieldInsights.FieldLatenciesResult.Error -> {
            logw(result.cause, "Could not fetch field latencies for project $projectName")
          }

          is FieldInsights.FieldLatencies -> {
            fieldLatenciesByProject[projectName] = result
          }
        }
      }
      this@FieldInsightsService.fieldLatenciesByService = fieldLatenciesByProject
      refreshInspections()
    }
  }

  fun getLatency(serviceId: ApolloKotlinService.Id, typeName: String, fieldName: String): Double? {
    return fieldLatenciesByService[serviceId.toString()]?.getLatency(parentType = typeName, fieldName = fieldName)
  }

  private fun refreshInspections() {
    DaemonCodeAnalyzer.getInstance(project).restart()
  }

  override fun dispose() {
    logd("project=${project.name}")
    coroutineScope.cancel()
    executor.shutdown()
  }
}

