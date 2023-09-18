package com.apollographql.ijplugin.telemetry

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel.TelemetryData.ServiceTelemetryData
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
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloJsExport
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloLanguageVersion
import com.apollographql.ijplugin.telemetry.TelemetryAttribute.ApolloNullableFieldStyle
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

    fun <T> List<ServiceTelemetryData>.notEmptySet(mapper: ServiceTelemetryData.() -> T?): Set<T>? = mapNotNull { mapper(it) }.toSet().takeIf { it.isNotEmpty() }
    serviceTelemetryData.notEmptySet { codegenModels }?.let { add(ApolloCodegenModels(it)) }
    serviceTelemetryData.notEmptySet { warnOnDeprecatedUsages }?.let { add(ApolloWarnOnDeprecatedUsages(it)) }
    serviceTelemetryData.notEmptySet { failOnWarnings }?.let { add(ApolloFailOnWarnings(it)) }
    serviceTelemetryData.notEmptySet { generateKotlinModels }?.let { add(ApolloGenerateKotlinModels(it)) }
    serviceTelemetryData.notEmptySet { languageVersion }?.let { add(ApolloLanguageVersion(it)) }
    serviceTelemetryData.notEmptySet { useSemanticNaming }?.let { add(ApolloUseSemanticNaming(it)) }
    serviceTelemetryData.notEmptySet { addJvmOverloads }?.let { add(ApolloAddJvmOverloads(it)) }
    serviceTelemetryData.notEmptySet { generateAsInternal }?.let { add(ApolloGenerateAsInternal(it)) }
    serviceTelemetryData.notEmptySet { generateFragmentImplementations }?.let { add(ApolloGenerateFragmentImplementations(it)) }
    serviceTelemetryData.notEmptySet { generateQueryDocument }?.let { add(ApolloGenerateQueryDocument(it)) }
    serviceTelemetryData.notEmptySet { generateSchema }?.let { add(ApolloGenerateSchema(it)) }
    serviceTelemetryData.notEmptySet { generateOptionalOperationVariables }?.let { add(ApolloGenerateOptionalOperationVariables(it)) }
    serviceTelemetryData.notEmptySet { generateDataBuilders }?.let { add(ApolloGenerateDataBuilders(it)) }
    serviceTelemetryData.notEmptySet { generateModelBuilders }?.let { add(ApolloGenerateModelBuilders(it)) }
    serviceTelemetryData.flatMap { it.generateMethods.orEmpty() }.toSet().let { if (it.isNotEmpty()) add(ApolloGenerateMethods(it)) }

    serviceTelemetryData.notEmptySet { generatePrimitiveTypes }?.let { add(ApolloGeneratePrimitiveTypes(it)) }
    serviceTelemetryData.notEmptySet { generateInputBuilders }?.let { add(ApolloGenerateInputBuilders(it)) }
    serviceTelemetryData.notEmptySet { nullableFieldStyle }?.let { add(ApolloNullableFieldStyle(it)) }
    serviceTelemetryData.notEmptySet { decapitalizeFields }?.let { add(ApolloDecapitalizeFields(it)) }
    serviceTelemetryData.notEmptySet { jsExport }?.let { add(ApolloJsExport(it)) }
    serviceTelemetryData.notEmptySet { addTypename }?.let { add(ApolloAddTypename(it)) }
    serviceTelemetryData.notEmptySet { flattenModels }?.let { add(ApolloFlattenModels(it)) }
    serviceTelemetryData.notEmptySet { fieldsOnDisjointTypesMustMerge }?.let { add(ApolloFieldsOnDisjointTypesMustMerge(it)) }
    serviceTelemetryData.notEmptySet { generateApolloMetadata }?.let { add(ApolloGenerateApolloMetadata(it)) }


    serviceTelemetryData.flatMap { it.usedOptions }.toSet().let { if (it.isNotEmpty()) add(ApolloUsedOptions(it)) }
  }
}
