package com.apollographql.apollo.gradle.api

import java.io.File

interface ApolloGradleToolingModel {
  val versionMajor: Int
  val versionMinor: Int

  @Deprecated("Use projectPath instead", ReplaceWith("projectPath"))
  val projectName: String

  // Introduced in 1.3
  val projectPath: String

  val serviceInfos: List<ServiceInfo>

  // Introduced in 1.2
  val telemetryData: TelemetryData

  interface ServiceInfo {
    val name: String
    val schemaFiles: Set<File>
    val graphqlSrcDirs: Set<File>
    @Deprecated("Use upstreamProjectPaths instead", ReplaceWith("upstreamProjectPaths"))
    val upstreamProjects: Set<String>

    // Introduced in 1.3
    val upstreamProjectPaths: Set<String>

    // Introduced in 1.1
    val endpointUrl: String?

    // Introduced in 1.1
    val endpointHeaders: Map<String, String>?

    // Introduced in 1.4
    val useSemanticNaming: Boolean
  }

  interface TelemetryData {
    val gradleVersion: String?
    val androidMinSdk: Int?
    val androidTargetSdk: Int?
    val androidCompileSdk: String?
    val androidAgpVersion: String?
    val apolloServiceTelemetryData: List<ServiceTelemetryData>
    val apolloGenerateSourcesDuringGradleSync: Boolean?
    val apolloLinkSqlite: Boolean?
    val apolloServiceCount: Int

    interface ServiceTelemetryData {
      val codegenModels: String?
      val warnOnDeprecatedUsages: Boolean?
      val failOnWarnings: Boolean?
      val operationManifestFormat: String?
      val generateKotlinModels: Boolean?
      val languageVersion: String?
      val useSemanticNaming: Boolean?
      val addJvmOverloads: Boolean?
      val generateAsInternal: Boolean?
      val generateFragmentImplementations: Boolean?
      val generateQueryDocument: Boolean?
      val generateSchema: Boolean?
      val generateOptionalOperationVariables: Boolean?
      val generateDataBuilders: Boolean?
      val generateModelBuilders: Boolean?
      val generateMethods: List<String>?
      val generatePrimitiveTypes: Boolean?
      val generateInputBuilders: Boolean?
      val nullableFieldStyle: String?
      val decapitalizeFields: Boolean?
      val jsExport: Boolean?
      val addTypename: String?
      val flattenModels: Boolean?
      val fieldsOnDisjointTypesMustMerge: Boolean?
      val generateApolloMetadata: Boolean?
      val usedOptions: Set<String>
    }
  }

  companion object {
    /**
     * Current major version of the tooling model.
     * Increment this value when the model changes in incompatible ways.
     * Adding properties / functions is compatible, whereas deleting, renaming, changing types or signatures is incompatible.
     */
    const val VERSION_MAJOR = 1

    /**
     * Current minor version of the tooling model.
     * Increment this value when the model changes in compatible ways (additions).
     */
    internal const val VERSION_MINOR = 4
  }
}
