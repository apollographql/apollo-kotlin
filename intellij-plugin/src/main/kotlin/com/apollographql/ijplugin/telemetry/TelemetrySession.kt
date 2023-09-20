package com.apollographql.ijplugin.telemetry

import java.time.Instant

sealed class TelemetryProperty(
    val type: String,
    val parameters: Any?,
) {
  override fun toString(): String {
    return "TelemetryProperty(type='$type', parameters=$parameters)"
  }

  /**
   * Gradle dependencies used by the project.
   */
  class Dependency(
      group: String,
      artifact: String,
      version: String,
  ) : TelemetryProperty("dependency_$group:$artifact", mapOf("version" to version))

  /**
   * Version of Kotlin (per kotlin-stdlib).
   */
  class KotlinVersion(version: String) : TelemetryProperty("kotlin_version", version)

  /**
   * Version of Compose (per androidx.compose.runtime).
   */
  class ComposeVersion(version: String) : TelemetryProperty("compose_version", version)

  /**
   * Version of Gradle.
   */
  class GradleVersion(version: String) : TelemetryProperty("gradle_version", version)

  /**
   * Android minSdk value.
   */
  class AndroidMinSdk(version: Int) : TelemetryProperty("android_min_sdk", version)

  /**
   * Android targetSdk value.
   */
  class AndroidTargetSdk(version: Int) : TelemetryProperty("android_target_sdk", version)

  /**
   * Android compileSdk value.
   */
  class AndroidCompileSdk(version: String) : TelemetryProperty("android_compile_sdk", version)

  /**
   * Version of the Android Gradle plugin.
   */
  class AndroidGradlePluginVersion(version: String) : TelemetryProperty("android_agp_version", version)

  /**
   * Value of the Apollo Kotlin option `codegenModels` if set.
   */
  class ApolloCodegenModels(codegenModels: String) : TelemetryProperty("ak_codegen_models", codegenModels)

  /**
   * Value of the Apollo Kotlin option `warnOnDeprecatedUsages` if set.
   */
  class ApolloWarnOnDeprecatedUsages(warnOnDeprecatedUsages: Boolean) : TelemetryProperty("ak_warn_on_deprecated_usages", warnOnDeprecatedUsages)

  /**
   * Value of the Apollo Kotlin option `failOnWarnings` if set.
   */
  class ApolloFailOnWarnings(failOnWarnings: Boolean) : TelemetryProperty("ak_fail_on_warnings", failOnWarnings)

  /**
   * Value of the Apollo Kotlin option `generateKotlinModels` if set.
   */
  class ApolloGenerateKotlinModels(generateKotlinModels: Boolean) : TelemetryProperty("ak_generate_kotlin_models", generateKotlinModels)

  /**
   * Value of the Apollo Kotlin option `languageVersion` if set.
   */
  class ApolloLanguageVersion(languageVersion: String) : TelemetryProperty("ak_language_version", languageVersion)

  /**
   * Value of the Apollo Kotlin option `useSemanticNaming` if set.
   */
  class ApolloUseSemanticNaming(useSemanticNaming: Boolean) : TelemetryProperty("ak_use_semantic_naming", useSemanticNaming)

  /**
   * Value of the Apollo Kotlin option `addJvmOverloads` if set.
   */
  class ApolloAddJvmOverloads(addJvmOverloads: Boolean) : TelemetryProperty("ak_add_jvm_overloads", addJvmOverloads)

  /**
   * Value of the Apollo Kotlin option `generateAsInternal` if set.
   */
  class ApolloGenerateAsInternal(generateAsInternal: Boolean) : TelemetryProperty("ak_generate_as_internal", generateAsInternal)

  /**
   * Value of the Apollo Kotlin option `generateFragmentImplementations` if set.
   */
  class ApolloGenerateFragmentImplementations(generateFragmentImplementations: Boolean) : TelemetryProperty("ak_generate_fragment_implementations", generateFragmentImplementations)

  /**
   * Value of the Apollo Kotlin option `generateQueryDocument` if set.
   */
  class ApolloGenerateQueryDocument(generateQueryDocument: Boolean) : TelemetryProperty("ak_generate_query_document", generateQueryDocument)

  /**
   * Value of the Apollo Kotlin option `generateSchema` if set.
   */
  class ApolloGenerateSchema(generateSchema: Boolean) : TelemetryProperty("ak_generate_schema", generateSchema)

  /**
   * Value of the Apollo Kotlin option `generateOptionalOperationVariables` if set.
   */
  class ApolloGenerateOptionalOperationVariables(generateOptionalOperationVariables: Boolean) : TelemetryProperty("ak_generate_optional_operation_variables", generateOptionalOperationVariables)

  /**
   * Value of the Apollo Kotlin option `generateDataBuilders` if set.
   */
  class ApolloGenerateDataBuilders(generateDataBuilders: Boolean) : TelemetryProperty("ak_generate_data_builders", generateDataBuilders)

  /**
   * Value of the Apollo Kotlin option `generateModelBuilders` if set.
   */
  class ApolloGenerateModelBuilders(generateModelBuilders: Boolean) : TelemetryProperty("ak_generate_model_builders", generateModelBuilders)

  /**
   * Value of the Apollo Kotlin option `generateMethods` if set.
   */
  class ApolloGenerateMethods(generateMethods: List<String>) : TelemetryProperty("ak_generate_methods", generateMethods)

  /**
   * Value of the Apollo Kotlin option `generatePrimitiveTypes` if set.
   */
  class ApolloGeneratePrimitiveTypes(generatePrimitiveTypes: Boolean) : TelemetryProperty("ak_generate_primitive_types", generatePrimitiveTypes)

  /**
   * Value of the Apollo Kotlin option `generateInputBuilders` if set.
   */
  class ApolloGenerateInputBuilders(generateInputBuilders: Boolean) : TelemetryProperty("ak_generate_input_builders", generateInputBuilders)

  /**
   * Value of the Apollo Kotlin option `nullableFieldStyle` if set.
   */
  class ApolloNullableFieldStyle(nullableFieldStyle: String) : TelemetryProperty("ak_nullable_field_style", nullableFieldStyle)

  /**
   * Value of the Apollo Kotlin option `decapitalizeFields` if set.
   */
  class ApolloDecapitalizeFields(decapitalizeFields: Boolean) : TelemetryProperty("ak_decapitalize_fields", decapitalizeFields)

  /**
   * Value of the Apollo Kotlin option `jsExport` if set.
   */
  class ApolloJsExport(jsExport: Boolean) : TelemetryProperty("ak_js_export", jsExport)

  /**
   * Value of the Apollo Kotlin option `addTypename` if set.
   */
  class ApolloAddTypename(addTypename: String) : TelemetryProperty("ak_add_typename", addTypename)

  /**
   * Value of the Apollo Kotlin option `flattenModels` if set.
   */
  class ApolloFlattenModels(flattenModels: Boolean) : TelemetryProperty("ak_flatten_models", flattenModels)

  /**
   * Value of the Apollo Kotlin option `fieldsOnDisjointTypesMustMerge` if set.
   */
  class ApolloFieldsOnDisjointTypesMustMerge(fieldsOnDisjointTypesMustMerge: Boolean) : TelemetryProperty("ak_fields_on_disjoint_types_must_merge", fieldsOnDisjointTypesMustMerge)

  /**
   * Value of the Apollo Kotlin option `generateApolloMetadata` if set.
   */
  class ApolloGenerateApolloMetadata(generateApolloMetadata: Boolean) : TelemetryProperty("ak_generate_apollo_metadata", generateApolloMetadata)

  /**
   * Other Apollo Kotlin Gradle plugin options that are used for which we don't care about the value.
   */
  class ApolloUsedOptions(otherOptions: Set<String>) : TelemetryProperty("ak_used_options", otherOptions)

  /**
   * Value of the Apollo Kotlin option `generateSourcesDuringGradleSync` if set.
   */
  class ApolloGenerateSourcesDuringGradleSync(generateSourcesDuringGradleSync: Boolean) : TelemetryProperty("ak_generate_sources_during_gradle_sync", generateSourcesDuringGradleSync)

  /**
   * Value of the Apollo Kotlin option `linkSqlite` if set.
   */
  class ApolloLinkSqlite(linkSqlite: Boolean) : TelemetryProperty("ak_link_sqlite", linkSqlite)

  /**
   * Value of the Apollo Kotlin option `useAntlr` if set.
   */
  class ApolloUseAntlr(useAntlr: Boolean) : TelemetryProperty("ak_use_antlr", useAntlr)

  /**
   * Number of defined services.
   */
  class ApolloServiceCount(serviceCount: Int) : TelemetryProperty("ak_service_count", serviceCount)

  /**
   * Total number of Gradle modules (including the root module).
   */
  class GradleModuleCount(moduleCount: Int) : TelemetryProperty("gradle_module_count", moduleCount)

  /**
   * Number of Apollo Kotlin modules (modules that apply the Apollo Kotlin Gradle plugin).
   */
  class ApolloKotlinModuleCount(moduleCount: Int) : TelemetryProperty("ak_module_count", moduleCount)

  /**
   * Name and version of the IDE.
   */
  class IdeVersion(version: String) : TelemetryProperty("ide_version", version)

  /**
   * Version of the Apollo Kotlin IntelliJ plugin.
   */
  class ApolloIjPluginVersion(version: String) : TelemetryProperty("akij_version", version)

  /**
   * Whether the Apollo Kotlin IntelliJ plugin option `automaticCodegenTriggering` is enabled.
   */
  class ApolloIjPluginAutomaticCodegenTriggering(automaticCodegenTriggering: Boolean) : TelemetryProperty("akij_automatic_codegen_triggering", automaticCodegenTriggering)

  /**
   * Whether the Apollo Kotlin IntelliJ plugin option `contributeConfigurationToGraphqlPlugin` is enabled.
   */
  class ApolloIjPluginContributeConfigurationToGraphqlPlugin(contributeConfigurationToGraphqlPlugin: Boolean) : TelemetryProperty("akij_contribute_configuration_to_graphql_plugin", contributeConfigurationToGraphqlPlugin)

  /**
   * Whether any GraphOS API key are configured in the Apollo Kotlin IntelliJ plugin.
   */
  class ApolloIjPluginHasConfiguredGraphOsApiKeys(hasConfiguredGraphOsApiKeys: Boolean) : TelemetryProperty("akij_has_configured_graphos_api_keys", hasConfiguredGraphOsApiKeys)

  /**
   * Value of the `threshold` option of the 'High latency field' inspection of the Apollo Kotlin IntelliJ plugin.
   */
  class ApolloIjPluginHighLatencyFieldThreshold(threshold: Int) : TelemetryProperty("akij_high_latency_field_threshold", threshold)
}

sealed class TelemetryEvent(
    val type: String,
    val parameters: Any?,
) {
  val date: Instant = Instant.now()

  override fun toString(): String {
    return "TelemetryEvent(date=$date, type='$type', parameters=$parameters)"
  }

  // TODO
  class ExampleEvent(parameters: Any?) : TelemetryEvent("example", parameters)
}

class TelemetryEventList {
  private val _events: MutableList<TelemetryEvent> = mutableListOf()
  val events: List<TelemetryEvent> = _events

  fun addEvent(telemetryEvent: TelemetryEvent) {
    _events.add(telemetryEvent)
  }

  fun clear() {
    _events.clear()
  }
}

data class TelemetrySession(
    val instanceId: String,
    val properties: Set<TelemetryProperty>,
    val events: List<TelemetryEvent>,
)
