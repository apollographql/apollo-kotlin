package com.apollographql.ijplugin.telemetry

import java.time.Instant

sealed class TelemetryAttribute(
    val type: String,
    val parameters: Any?,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as TelemetryAttribute
    return type == other.type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }

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
   * Value(s) of the Apollo Kotlin option `codegenModels` if set.
   */
  class ApolloCodegenModels(codegenModels: Set<String>) : TelemetryAttribute("ak_codegen_models", codegenModels)

  /**
   * Value(s) of the Apollo Kotlin option `warnOnDeprecatedUsages` if set.
   */
  class ApolloWarnOnDeprecatedUsages(warnOnDeprecatedUsages: Set<Boolean>) : TelemetryAttribute("ak_warn_on_deprecated_usages", warnOnDeprecatedUsages)

  /**
   * Value(s) of the Apollo Kotlin option `failOnWarnings` if set.
   */
  class ApolloFailOnWarnings(failOnWarnings: Set<Boolean>) : TelemetryAttribute("ak_fail_on_warnings", failOnWarnings)

  /**
   * Value(s) of the Apollo Kotlin option `generateKotlinModels` if set.
   */
  class ApolloGenerateKotlinModels(generateKotlinModels: Set<Boolean>) : TelemetryAttribute("ak_generate_kotlin_models", generateKotlinModels)

  /**
   * Value(s) of the Apollo Kotlin option `languageVersion` if set.
   */
  class ApolloLanguageVersion(languageVersion: Set<String>) : TelemetryAttribute("ak_language_version", languageVersion)

  /**
   * Value(s) of the Apollo Kotlin option `useSemanticNaming` if set.
   */
  class ApolloUseSemanticNaming(useSemanticNaming: Set<Boolean>) : TelemetryAttribute("ak_use_semantic_naming", useSemanticNaming)

  /**
   * Value(s) of the Apollo Kotlin option `addJvmOverloads` if set.
   */
  class ApolloAddJvmOverloads(addJvmOverloads: Set<Boolean>) : TelemetryAttribute("ak_add_jvm_overloads", addJvmOverloads)

  /**
   * Value(s) of the Apollo Kotlin option `generateAsInternal` if set.
   */
  class ApolloGenerateAsInternal(generateAsInternal: Set<Boolean>) : TelemetryAttribute("ak_generate_as_internal", generateAsInternal)

  /**
   * Value(s) of the Apollo Kotlin option `generateFragmentImplementations` if set.
   */
  class ApolloGenerateFragmentImplementations(generateFragmentImplementations: Set<Boolean>) : TelemetryAttribute("ak_generate_fragment_implementations", generateFragmentImplementations)

  /**
   * Value(s) of the Apollo Kotlin option `generateQueryDocument` if set.
   */
  class ApolloGenerateQueryDocument(generateQueryDocument: Set<Boolean>) : TelemetryAttribute("ak_generate_query_document", generateQueryDocument)

  /**
   * Value(s) of the Apollo Kotlin option `generateSchema` if set.
   */
  class ApolloGenerateSchema(generateSchema: Set<Boolean>) : TelemetryAttribute("ak_generate_schema", generateSchema)

  /**
   * Value(s) of the Apollo Kotlin option `generateOptionalOperationVariables` if set.
   */
  class ApolloGenerateOptionalOperationVariables(generateOptionalOperationVariables: Set<Boolean>) : TelemetryAttribute("ak_generate_optional_operation_variables", generateOptionalOperationVariables)

  /**
   * Value(s) of the Apollo Kotlin option `generateDataBuilders` if set.
   */
  class ApolloGenerateDataBuilders(generateDataBuilders: Set<Boolean>) : TelemetryAttribute("ak_generate_data_builders", generateDataBuilders)

  /**
   * Value(s) of the Apollo Kotlin option `generateModelBuilders` if set.
   */
  class ApolloGenerateModelBuilders(generateModelBuilders: Set<Boolean>) : TelemetryAttribute("ak_generate_model_builders", generateModelBuilders)

  /**
   * Value(s) of the Apollo Kotlin option `generateMethods` if set.
   */
  class ApolloGenerateMethods(generateMethods: Set<String>) : TelemetryAttribute("ak_generate_methods", generateMethods)

  /**
   * Value(s) of the Apollo Kotlin option `generatePrimitiveTypes` if set.
   */
  class ApolloGeneratePrimitiveTypes(generatePrimitiveTypes: Set<Boolean>) : TelemetryAttribute("ak_generate_primitive_types", generatePrimitiveTypes)

  /**
   * Value(s) of the Apollo Kotlin option `generateInputBuilders` if set.
   */
  class ApolloGenerateInputBuilders(generateInputBuilders: Set<Boolean>) : TelemetryAttribute("ak_generate_input_builders", generateInputBuilders)

  /**
   * Value(s) of the Apollo Kotlin option `nullableFieldStyle` if set.
   */
  class ApolloNullableFieldStyle(nullableFieldStyle: Set<String>) : TelemetryAttribute("ak_nullable_field_style", nullableFieldStyle)

  /**
   * Value(s) of the Apollo Kotlin option `decapitalizeFields` if set.
   */
  class ApolloDecapitalizeFields(decapitalizeFields: Set<Boolean>) : TelemetryAttribute("ak_decapitalize_fields", decapitalizeFields)

  /**
   * Value(s) of the Apollo Kotlin option `jsExport` if set.
   */
  class ApolloJsExport(jsExport: Set<Boolean>) : TelemetryAttribute("ak_js_export", jsExport)

  /**
   * Value(s) of the Apollo Kotlin option `addTypename` if set.
   */
  class ApolloAddTypename(addTypename: Set<String>) : TelemetryAttribute("ak_add_typename", addTypename)

  /**
   * Value(s) of the Apollo Kotlin option `flattenModels` if set.
   */
  class ApolloFlattenModels(flattenModels: Set<Boolean>) : TelemetryAttribute("ak_flatten_models", flattenModels)

  /**
   * Value(s) of the Apollo Kotlin option `fieldsOnDisjointTypesMustMerge` if set.
   */
  class ApolloFieldsOnDisjointTypesMustMerge(fieldsOnDisjointTypesMustMerge: Set<Boolean>) : TelemetryAttribute("ak_fields_on_disjoint_types_must_merge", fieldsOnDisjointTypesMustMerge)

  /**
   * Value(s) of the Apollo Kotlin option `generateApolloMetadata` if set.
   */
  class ApolloGenerateApolloMetadata(generateApolloMetadata: Set<Boolean>) : TelemetryAttribute("ak_generate_apollo_metadata", generateApolloMetadata)

  /**
   * Other Apollo Kotlin Gradle plugin options that are used for which we don't care about the value.
   */
  class ApolloUsedOptions(otherOptions: Set<String>) : TelemetryAttribute("ak_used_options", otherOptions)
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
