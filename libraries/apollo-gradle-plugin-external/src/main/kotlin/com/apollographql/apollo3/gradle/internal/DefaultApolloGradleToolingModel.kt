package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import java.io.File
import java.io.Serializable

internal data class DefaultApolloGradleToolingModel(
    override val projectName: String,
    override val serviceInfos: List<ApolloGradleToolingModel.ServiceInfo>,
    override val telemetryData: ApolloGradleToolingModel.TelemetryData,
) : ApolloGradleToolingModel, Serializable {
  override val versionMajor: Int = ApolloGradleToolingModel.VERSION_MAJOR
  override val versionMinor: Int = ApolloGradleToolingModel.VERSION_MINOR
}

internal data class DefaultServiceInfo(
    override val name: String,
    override val schemaFiles: Set<File>,
    override val graphqlSrcDirs: Set<File>,
    override val upstreamProjects: Set<String>,
    override val endpointUrl: String?,
    override val endpointHeaders: Map<String, String>?,
) : ApolloGradleToolingModel.ServiceInfo, Serializable

internal data class DefaultTelemetryData(
    override val gradleVersion: String?,
    override val androidMinSdk: Int?,
    override val androidTargetSdk: Int?,
    override val androidCompileSdk: Int?,
) : ApolloGradleToolingModel.TelemetryData, Serializable
