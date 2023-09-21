package com.apollographql.ijplugin.telemetry

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.settings.settingsState
import com.apollographql.ijplugin.studio.fieldinsights.ApolloFieldInsightsInspection
import com.apollographql.ijplugin.telemetry.TelemetryProperty.AndroidCompileSdk
import com.apollographql.ijplugin.telemetry.TelemetryProperty.AndroidGradlePluginVersion
import com.apollographql.ijplugin.telemetry.TelemetryProperty.AndroidMinSdk
import com.apollographql.ijplugin.telemetry.TelemetryProperty.AndroidTargetSdk
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloAddJvmOverloads
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloAddTypename
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloCodegenModels
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloDecapitalizeFields
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloFailOnWarnings
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloFieldsOnDisjointTypesMustMerge
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloFlattenModels
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateApolloMetadata
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateAsInternal
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateDataBuilders
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateFragmentImplementations
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateInputBuilders
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateKotlinModels
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateMethods
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateModelBuilders
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateOptionalOperationVariables
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGeneratePrimitiveTypes
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateQueryDocument
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateSchema
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloGenerateSourcesDuringGradleSync
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloIjPluginAutomaticCodegenTriggering
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloIjPluginContributeConfigurationToGraphqlPlugin
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloIjPluginHasConfiguredGraphOsApiKeys
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloIjPluginHighLatencyFieldThreshold
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloIjPluginVersion
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloJsExport
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloKotlinModuleCount
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloLanguageVersion
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloLinkSqlite
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloNullableFieldStyle
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloServiceCount
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloUseSemanticNaming
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloUsedOptions
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloWarnOnDeprecatedUsages
import com.apollographql.ijplugin.telemetry.TelemetryProperty.GradleModuleCount
import com.apollographql.ijplugin.telemetry.TelemetryProperty.GradleVersion
import com.apollographql.ijplugin.telemetry.TelemetryProperty.IdeVersion
import com.apollographql.ijplugin.util.NOTIFICATION_GROUP_ID_TELEMETRY
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.createNotification
import com.apollographql.ijplugin.util.logd
import com.intellij.ProjectTopics
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager


private const val DATA_PRIVACY_URL = "https://www.apollographql.com/docs/graphos/data-privacy/"

/**
 * TODO remove this
 */
const val TELEMETRY_ENABLED = false

@Service(Service.Level.PROJECT)
class TelemetryService(
    private val project: Project,
) : Disposable {

  var gradleToolingModels: Set<ApolloGradleToolingModel> = emptySet()
  var gradleModuleCount: Int? = null
  var apolloKotlinModuleCount: Int? = null

  private val telemetryEventList: TelemetryEventList = TelemetryEventList()

  private var projectLibraries: Set<Dependency> = emptySet()

  init {
    logd("project=${project.name}")
    onLibrariesChanged()
    startObserveLibraries()

    maybeShowTelemetryOptOutDialog()
  }

  private fun startObserveLibraries() {
    logd()
    project.messageBus.connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        logd("event=$event")
        onLibrariesChanged()
      }
    })
  }

  private fun onLibrariesChanged() {
    logd()
    projectLibraries = project.getProjectDependencies()
  }

  fun logEvent(telemetryEvent: TelemetryEvent) {
    telemetryEventList.addEvent(telemetryEvent)
  }

  private fun buildTelemetrySession(): TelemetrySession {
    val properties = buildSet {
      addAll(projectLibraries.toTelemetryProperties())
      addAll(gradleToolingModels.flatMap { it.toTelemetryProperties() })
      gradleModuleCount?.let { add(GradleModuleCount(it)) }
      apolloKotlinModuleCount?.let { add(ApolloKotlinModuleCount(it)) }
      addAll(getIdeTelemetryProperties())
    }
    return TelemetrySession(
        instanceId = project.settingsState.telemetryInstanceId,
        properties = properties,
        events = telemetryEventList.events,
    )
  }

  private fun getIdeTelemetryProperties(): Set<TelemetryProperty> = buildSet {
    var appName = ApplicationInfoEx.getInstanceEx().fullApplicationName
    ApplicationNamesInfo.getInstance().editionName?.let { edition ->
      appName += " ($edition)"
    }
    add(IdeVersion(appName))
    System.getProperties().getProperty("os.name")?.let { add(TelemetryProperty.IdeOS(it)) }
    PluginManagerCore.getPlugin(PluginId.getId("com.apollographql.ijplugin"))?.version?.let { add(ApolloIjPluginVersion(it)) }
    add(ApolloIjPluginAutomaticCodegenTriggering(project.settingsState.automaticCodegenTriggering))
    add(ApolloIjPluginContributeConfigurationToGraphqlPlugin(project.settingsState.contributeConfigurationToGraphqlPlugin))
    add(ApolloIjPluginHasConfiguredGraphOsApiKeys(project.settingsState.apolloKotlinServiceConfigurations.isNotEmpty()))
    ProjectInspectionProfileManager.getInstance(project).currentProfile.getInspectionTool("ApolloFieldInsights", project)?.tool?.cast<ApolloFieldInsightsInspection>()?.let {
      add(ApolloIjPluginHighLatencyFieldThreshold(it.thresholdMs))
    }
  }

  override fun dispose() {
    logd("project=${project.name}")
  }

  fun sendTelemetry() {
    // TODO
    val telemetrySession = buildTelemetrySession()
    logd("telemetrySession=$telemetrySession")
    telemetrySession.properties.forEach { logd(it) }
    logd("---")
    telemetrySession.events.forEach { logd(it) }
  }

  private fun maybeShowTelemetryOptOutDialog() {
    if (!TELEMETRY_ENABLED) return
    if (project.settingsState.hasShownTelemetryOptOutDialog) return
    project.settingsState.hasShownTelemetryOptOutDialog = true
    createNotification(
        notificationGroupId = NOTIFICATION_GROUP_ID_TELEMETRY,
        title = ApolloBundle.message("telemetry.optOutDialog.title"),
        content = ApolloBundle.message("telemetry.optOutDialog.content"),
        type = NotificationType.INFORMATION,
        NotificationAction.create(ApolloBundle.message("telemetry.optOutDialog.optOut")) { _, notification ->
          project.settingsState.telemetryEnabled = false
          notification.expire()
        },
        NotificationAction.create(ApolloBundle.message("telemetry.optOutDialog.learnMore")) { _, _ ->
          BrowserUtil.browse(DATA_PRIVACY_URL, project)
        },
    )
        .apply {
          icon = ApolloIcons.Action.ApolloColor
        }
        .notify(project)
  }
}

val Project.telemetryService get() = service<TelemetryService>()

private fun ApolloGradleToolingModel.toTelemetryProperties(): Set<TelemetryProperty> = buildSet {
  // telemetryData was introduced in 1.2, accessing it on an older version will throw an exception
  if (versionMajor == 1 && versionMinor < 2) return@buildSet
  with(telemetryData) {
    gradleVersion?.let { add(GradleVersion(it)) }

    androidMinSdk?.let { add(AndroidMinSdk(it)) }
    androidTargetSdk?.let { add(AndroidTargetSdk(it)) }
    androidCompileSdk?.let { add(AndroidCompileSdk(it)) }
    androidAgpVersion?.let { add(AndroidGradlePluginVersion(it)) }

    apolloGenerateSourcesDuringGradleSync?.let { add(ApolloGenerateSourcesDuringGradleSync(it)) }
    apolloLinkSqlite?.let { add(ApolloLinkSqlite(it)) }
    add(ApolloServiceCount(apolloServiceCount))

    apolloServiceTelemetryData.forEach {
      it.operationManifestFormat?.let { add(ApolloCodegenModels(it)) }
      it.warnOnDeprecatedUsages?.let { add(ApolloWarnOnDeprecatedUsages(it)) }
      it.failOnWarnings?.let { add(ApolloFailOnWarnings(it)) }
      it.generateKotlinModels?.let { add(ApolloGenerateKotlinModels(it)) }
      it.languageVersion?.let { add(ApolloLanguageVersion(it)) }
      it.useSemanticNaming?.let { add(ApolloUseSemanticNaming(it)) }
      it.addJvmOverloads?.let { add(ApolloAddJvmOverloads(it)) }
      it.generateAsInternal?.let { add(ApolloGenerateAsInternal(it)) }
      it.generateFragmentImplementations?.let { add(ApolloGenerateFragmentImplementations(it)) }
      it.generateQueryDocument?.let { add(ApolloGenerateQueryDocument(it)) }
      it.generateSchema?.let { add(ApolloGenerateSchema(it)) }
      it.generateOptionalOperationVariables?.let { add(ApolloGenerateOptionalOperationVariables(it)) }
      it.generateDataBuilders?.let { add(ApolloGenerateDataBuilders(it)) }
      it.generateModelBuilders?.let { add(ApolloGenerateModelBuilders(it)) }
      it.generateMethods?.let { add(ApolloGenerateMethods(it)) }
      it.generatePrimitiveTypes?.let { add(ApolloGeneratePrimitiveTypes(it)) }
      it.generateInputBuilders?.let { add(ApolloGenerateInputBuilders(it)) }
      it.nullableFieldStyle?.let { add(ApolloNullableFieldStyle(it)) }
      it.decapitalizeFields?.let { add(ApolloDecapitalizeFields(it)) }
      it.jsExport?.let { add(ApolloJsExport(it)) }
      it.addTypename?.let { add(ApolloAddTypename(it)) }
      it.flattenModels?.let { add(ApolloFlattenModels(it)) }
      it.fieldsOnDisjointTypesMustMerge?.let { add(ApolloFieldsOnDisjointTypesMustMerge(it)) }
      it.generateApolloMetadata?.let { add(ApolloGenerateApolloMetadata(it)) }
      add(ApolloUsedOptions(it.usedOptions))
    }
  }
}
