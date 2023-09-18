package com.apollographql.apollo3.gradle.api

import java.io.File

interface ApolloGradleToolingModel {
  val versionMajor: Int
  val versionMinor: Int

  val projectName: String
  val serviceInfos: List<ServiceInfo>

  // Introduced in 1.2
  val telemetryData: TelemetryData

  interface ServiceInfo {
    val name: String
    val schemaFiles: Set<File>
    val graphqlSrcDirs: Set<File>
    val upstreamProjects: Set<String>

    // Introduced in 1.1
    val endpointUrl: String?

    // Introduced in 1.1
    val endpointHeaders: Map<String, String>?
  }

  interface TelemetryData {
    val gradleVersion: String?
    val androidMinSdk: Int?
    val androidTargetSdk: Int?
    val androidCompileSdk: Int?
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
    const val VERSION_MINOR = 2
  }
}
