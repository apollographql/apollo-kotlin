package com.apollographql.ijplugin.telemetry

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
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
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloJsExport
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloKotlinModuleCount
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloLanguageVersion
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloLinkSqlite
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloNullableFieldStyle
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloServiceCount
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloUseAntlr
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloUseSemanticNaming
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloUsedOptions
import com.apollographql.ijplugin.telemetry.TelemetryProperty.ApolloWarnOnDeprecatedUsages
import com.apollographql.ijplugin.telemetry.TelemetryProperty.GradleModuleCount
import com.apollographql.ijplugin.telemetry.TelemetryProperty.GradleVersion
import com.apollographql.ijplugin.util.logd
import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener

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

  fun addEvent(telemetryEvent: TelemetryEvent) {
    telemetryEventList.addEvent(telemetryEvent)
  }

  private fun buildTelemetrySession(): TelemetrySession {
    val properties = buildSet {
      addAll(projectLibraries.toTelemetryProperties())
      addAll(gradleToolingModels.flatMap { it.toTelemetryProperties() })
      gradleModuleCount?.let { add(GradleModuleCount(it)) }
      apolloKotlinModuleCount?.let { add(ApolloKotlinModuleCount(it)) }
    }
    return TelemetrySession(
        instanceId = "TODO", // TODO
        properties = properties,
        events = telemetryEventList.events,
    )
  }

  override fun dispose() {
    logd("project=${project.name}")
  }

  fun sendTelemetry() {
    // TODO
    val telemetrySession = buildTelemetrySession()
    logd("telemetrySession=$telemetrySession")
    telemetrySession.properties.forEach { logd(it) }
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
    apolloUseAntlr?.let { add(ApolloUseAntlr(it)) }
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
