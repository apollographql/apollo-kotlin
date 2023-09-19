package com.apollographql.ijplugin.telemetry

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidCompileSdk
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidGradlePluginVersion
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidMinSdk
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.AndroidTargetSdk
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloAddJvmOverloads
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloAddTypename
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloCodegenModels
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloDecapitalizeFields
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloFailOnWarnings
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloFieldsOnDisjointTypesMustMerge
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloFlattenModels
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateApolloMetadata
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateAsInternal
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateDataBuilders
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateFragmentImplementations
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateInputBuilders
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateKotlinModels
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateMethods
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateModelBuilders
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateOptionalOperationVariables
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGeneratePrimitiveTypes
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateQueryDocument
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateSchema
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloGenerateSourcesDuringGradleSync
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloJsExport
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloLanguageVersion
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloLinkSqlite
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloNullableFieldStyle
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloServiceCount
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloUseAntlr
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloUseSemanticNaming
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloUsedOptions
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloWarnOnDeprecatedUsages
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.GradleVersion
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
    return TelemetrySession(
        instanceId = "TODO", // TODO
        attributes = projectLibraries.toTelemetryAttributes() + gradleToolingModels.flatMap { it.toTelemetryAttributes() }.toSet(),
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
    telemetrySession.attributes.forEach { logd(it) }
  }
}

val Project.telemetryService get() = service<TelemetryService>()

private fun ApolloGradleToolingModel.toTelemetryAttributes(): Set<TelemetryAttribute> = buildSet {
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
