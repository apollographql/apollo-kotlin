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
  private var fieldLatenciesByService = mapOf<ApolloKotlinService.Id, FieldInsights.FieldLatencies>()

  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var fetchLatenciesFuture: ScheduledFuture<*>? = null

  init {
    logd("project=${project.name}")
    startObservingSettings()
    startObservingApolloKotlinServices()
  }

  private fun startObservingSettings() {
    logd()
    project.messageBus.connect(this).subscribe(SettingsListener.TOPIC, object : SettingsListener {
      var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration> = project.settingsState.apolloKotlinServiceConfigurations
      override fun settingsChanged(settingsState: SettingsState) {
        val apolloKotlinServiceConfigurationsChanged = apolloKotlinServiceConfigurations != settingsState.apolloKotlinServiceConfigurations
        apolloKotlinServiceConfigurations = settingsState.apolloKotlinServiceConfigurations
        logd("apolloKotlinServiceConfigurationsChanged=$apolloKotlinServiceConfigurationsChanged")
        if (apolloKotlinServiceConfigurationsChanged) {
          scheduleFetchLatencies()
        }
      }
    })
  }

  private fun startObservingApolloKotlinServices() {
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
    val apolloKotlinServices = GradleToolingModelService.getApolloKotlinServices(project)
    val apolloKotlinServicesWithConfigurations: Map<ApolloKotlinService, ApolloKotlinServiceConfiguration> = apolloKotlinServices.associateWith { service ->
      project.settingsState.apolloKotlinServiceConfigurations.firstOrNull { configuration ->
        service.id == configuration.apolloKotlinServiceId
      }
    }.filterValues { it != null }.mapValues { it.value!! }
    val deferredLatenciesByService = apolloKotlinServicesWithConfigurations.mapNotNull { (service, configuration) ->
      configuration.graphOsApiKey?.let { apiKey ->
        service to coroutineScope.async {
          FieldInsights.fetchFieldLatencies(
              apiKey = apiKey,
              serviceId = configuration.graphOsGraphName,
          )
        }
      }
    }
    coroutineScope.launch {
      val fieldLatenciesByService = mutableMapOf<ApolloKotlinService.Id, FieldInsights.FieldLatencies>()
      for ((service, deferred) in deferredLatenciesByService) {
        val result = try {
          deferred.await()
        } catch (e: Exception) {
          FieldInsights.FieldLatenciesResult.Error(e)
        }
        when (result) {
          is FieldInsights.FieldLatenciesResult.Error -> {
            logw(result.cause, "Could not fetch field latencies for service ${service.id}")
          }

          is FieldInsights.FieldLatencies -> {
            fieldLatenciesByService[service.id] = result
          }
        }
      }
      this@FieldInsightsService.fieldLatenciesByService = fieldLatenciesByService
      refreshInspections()
    }
  }

  fun getLatency(serviceId: ApolloKotlinService.Id, typeName: String, fieldName: String): Double? {
    return fieldLatenciesByService[serviceId]?.getLatency(parentType = typeName, fieldName = fieldName)
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
