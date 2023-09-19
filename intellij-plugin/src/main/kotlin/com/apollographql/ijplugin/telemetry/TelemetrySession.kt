package com.apollographql.ijplugin.telemetry

import java.time.Instant

sealed class TelemetryAttribute(
    val type: String,
    val parameters: Any?,
) {
  override fun toString(): String {
    return "TelemetryAttribute(type='$type', parameters=$parameters)"
  }

  /**
   * Gradle dependencies used by the project.
   */
  class Dependency(
      group: String,
      artifact: String,
      version: String,
  ) : TelemetryAttribute("dependency_$group:$artifact", mapOf("version" to version))

  /**
   * Version of Kotlin (per kotlin-stdlib).
   */
  class KotlinVersion(version: String) : TelemetryAttribute("kotlin_version", version)

  /**
   * Version of Compose (per androidx.compose.runtime).
   */
  class ComposeVersion(version: String) : TelemetryAttribute("compose_version", version)

  /**
   * Version of Gradle.
   */
  class GradleVersion(version: String) : TelemetryAttribute("gradle_version", version)

  /**
   * Android minSdk value.
   */
  class AndroidMinSdk(version: Int) : TelemetryAttribute("android_min_sdk", version)

  /**
   * Android targetSdk value.
   */
  class AndroidTargetSdk(version: Int) : TelemetryAttribute("android_target_sdk", version)

  /**
   * Android compileSdk value.
   */
  class AndroidCompileSdk(version: String) : TelemetryAttribute("android_compile_sdk", version)

  /**
   * Version of the Android Gradle plugin.
   */
  class AndroidGradlePluginVersion(version: String) : TelemetryAttribute("android_agp_version", version)

  /**
   * Value of the Apollo Kotlin option `codegenModels` if set.
   */
  class ApolloCodegenModels(codegenModels: String) : TelemetryAttribute("ak_codegen_models", codegenModels)

  /**
   * Value of the Apollo Kotlin option `warnOnDeprecatedUsages` if set.
   */
  class ApolloWarnOnDeprecatedUsages(warnOnDeprecatedUsages: Boolean) : TelemetryAttribute("ak_warn_on_deprecated_usages", warnOnDeprecatedUsages)

  /**
   * Value of the Apollo Kotlin option `failOnWarnings` if set.
   */
  class ApolloFailOnWarnings(failOnWarnings: Boolean) : TelemetryAttribute("ak_fail_on_warnings", failOnWarnings)

  /**
   * Value of the Apollo Kotlin option `generateKotlinModels` if set.
   */
  class ApolloGenerateKotlinModels(generateKotlinModels: Boolean) : TelemetryAttribute("ak_generate_kotlin_models", generateKotlinModels)

  /**
   * Value of the Apollo Kotlin option `languageVersion` if set.
   */
  class ApolloLanguageVersion(languageVersion: String) : TelemetryAttribute("ak_language_version", languageVersion)

  /**
   * Value of the Apollo Kotlin option `useSemanticNaming` if set.
   */
  class ApolloUseSemanticNaming(useSemanticNaming: Boolean) : TelemetryAttribute("ak_use_semantic_naming", useSemanticNaming)

  /**
   * Value of the Apollo Kotlin option `addJvmOverloads` if set.
   */
  class ApolloAddJvmOverloads(addJvmOverloads: Boolean) : TelemetryAttribute("ak_add_jvm_overloads", addJvmOverloads)

  /**
   * Value of the Apollo Kotlin option `generateAsInternal` if set.
   */
  class ApolloGenerateAsInternal(generateAsInternal: Boolean) : TelemetryAttribute("ak_generate_as_internal", generateAsInternal)

  /**
   * Value of the Apollo Kotlin option `generateFragmentImplementations` if set.
   */
  class ApolloGenerateFragmentImplementations(generateFragmentImplementations: Boolean) : TelemetryAttribute("ak_generate_fragment_implementations", generateFragmentImplementations)

  /**
   * Value of the Apollo Kotlin option `generateQueryDocument` if set.
   */
  class ApolloGenerateQueryDocument(generateQueryDocument: Boolean) : TelemetryAttribute("ak_generate_query_document", generateQueryDocument)

  /**
   * Value of the Apollo Kotlin option `generateSchema` if set.
   */
  class ApolloGenerateSchema(generateSchema: Boolean) : TelemetryAttribute("ak_generate_schema", generateSchema)

  /**
   * Value of the Apollo Kotlin option `generateOptionalOperationVariables` if set.
   */
  class ApolloGenerateOptionalOperationVariables(generateOptionalOperationVariables: Boolean) : TelemetryAttribute("ak_generate_optional_operation_variables", generateOptionalOperationVariables)

  /**
   * Value of the Apollo Kotlin option `generateDataBuilders` if set.
   */
  class ApolloGenerateDataBuilders(generateDataBuilders: Boolean) : TelemetryAttribute("ak_generate_data_builders", generateDataBuilders)

  /**
   * Value of the Apollo Kotlin option `generateModelBuilders` if set.
   */
  class ApolloGenerateModelBuilders(generateModelBuilders: Boolean) : TelemetryAttribute("ak_generate_model_builders", generateModelBuilders)

  /**
   * Value of the Apollo Kotlin option `generateMethods` if set.
   */
  class ApolloGenerateMethods(generateMethods: List<String>) : TelemetryAttribute("ak_generate_methods", generateMethods)

  /**
   * Value of the Apollo Kotlin option `generatePrimitiveTypes` if set.
   */
  class ApolloGeneratePrimitiveTypes(generatePrimitiveTypes: Boolean) : TelemetryAttribute("ak_generate_primitive_types", generatePrimitiveTypes)

  /**
   * Value of the Apollo Kotlin option `generateInputBuilders` if set.
   */
  class ApolloGenerateInputBuilders(generateInputBuilders: Boolean) : TelemetryAttribute("ak_generate_input_builders", generateInputBuilders)

  /**
   * Value of the Apollo Kotlin option `nullableFieldStyle` if set.
   */
  class ApolloNullableFieldStyle(nullableFieldStyle: String) : TelemetryAttribute("ak_nullable_field_style", nullableFieldStyle)

  /**
   * Value of the Apollo Kotlin option `decapitalizeFields` if set.
   */
  class ApolloDecapitalizeFields(decapitalizeFields: Boolean) : TelemetryAttribute("ak_decapitalize_fields", decapitalizeFields)

  /**
   * Value of the Apollo Kotlin option `jsExport` if set.
   */
  class ApolloJsExport(jsExport: Boolean) : TelemetryAttribute("ak_js_export", jsExport)

  /**
   * Value of the Apollo Kotlin option `addTypename` if set.
   */
  class ApolloAddTypename(addTypename: String) : TelemetryAttribute("ak_add_typename", addTypename)

  /**
   * Value of the Apollo Kotlin option `flattenModels` if set.
   */
  class ApolloFlattenModels(flattenModels: Boolean) : TelemetryAttribute("ak_flatten_models", flattenModels)

  /**
   * Value of the Apollo Kotlin option `fieldsOnDisjointTypesMustMerge` if set.
   */
  class ApolloFieldsOnDisjointTypesMustMerge(fieldsOnDisjointTypesMustMerge: Boolean) : TelemetryAttribute("ak_fields_on_disjoint_types_must_merge", fieldsOnDisjointTypesMustMerge)

  /**
   * Value of the Apollo Kotlin option `generateApolloMetadata` if set.
   */
  class ApolloGenerateApolloMetadata(generateApolloMetadata: Boolean) : TelemetryAttribute("ak_generate_apollo_metadata", generateApolloMetadata)

  /**
   * Other Apollo Kotlin Gradle plugin options that are used for which we don't care about the value.
   */
  class ApolloUsedOptions(otherOptions: Set<String>) : TelemetryAttribute("ak_used_options", otherOptions)

  /**
   * Value of the Apollo Kotlin option `generateSourcesDuringGradleSync` if set.
   */
  class ApolloGenerateSourcesDuringGradleSync(generateSourcesDuringGradleSync: Boolean) : TelemetryAttribute("ak_generate_sources_during_gradle_sync", generateSourcesDuringGradleSync)

  /**
   * Value of the Apollo Kotlin option `linkSqlite` if set.
   */
  class ApolloLinkSqlite(linkSqlite: Boolean) : TelemetryAttribute("ak_link_sqlite", linkSqlite)

  /**
   * Value of the Apollo Kotlin option `useAntlr` if set.
   */
  class ApolloUseAntlr(useAntlr: Boolean) : TelemetryAttribute("ak_use_antlr", useAntlr)

  /**
   * Number of defined services.
   */
  class ApolloServiceCount(serviceCount: Int) : TelemetryAttribute("ak_service_count", serviceCount)
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
    val attributes: Set<TelemetryAttribute>,
    val events: List<TelemetryEvent>,
)
