package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloGradleToolingModel
import java.io.File
import java.io.Serializable

internal data class DefaultApolloGradleToolingModel(
    @Deprecated("Use projectPath instead", replaceWith = ReplaceWith("projectPath"))
    override val projectName: String,
    override val projectPath: String,
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
    @Deprecated("Use upstreamProjectPaths instead", replaceWith = ReplaceWith("upstreamProjectPaths"))
    override val upstreamProjects: Set<String>,
    override val upstreamProjectPaths: Set<String>,
    override val endpointUrl: String?,
    override val endpointHeaders: Map<String, String>?,
    override val useSemanticNaming: Boolean,
) : ApolloGradleToolingModel.ServiceInfo, Serializable

internal data class DefaultTelemetryData(
    override val gradleVersion: String?,
    override val androidMinSdk: Int?,
    override val androidTargetSdk: Int?,
    override val androidCompileSdk: String?,
    override val androidAgpVersion: String?,
    override val apolloServiceTelemetryData: List<ApolloGradleToolingModel.TelemetryData.ServiceTelemetryData>,
    override val apolloGenerateSourcesDuringGradleSync: Boolean?,
    override val apolloLinkSqlite: Boolean?,
    override val apolloServiceCount: Int,
) : ApolloGradleToolingModel.TelemetryData, Serializable

internal data class DefaultServiceTelemetryData(
    override val codegenModels: String?,
    override val warnOnDeprecatedUsages: Boolean?,
    override val failOnWarnings: Boolean?,
    override val operationManifestFormat: String?,
    override val generateKotlinModels: Boolean?,
    override val languageVersion: String?,
    override val useSemanticNaming: Boolean?,
    override val addJvmOverloads: Boolean?,
    override val generateAsInternal: Boolean?,
    override val generateFragmentImplementations: Boolean?,
    override val generateQueryDocument: Boolean?,
    override val generateSchema: Boolean?,
    override val generateOptionalOperationVariables: Boolean?,
    override val generateDataBuilders: Boolean?,
    override val generateModelBuilders: Boolean?,
    override val generateMethods: List<String>?,
    override val generatePrimitiveTypes: Boolean?,
    override val generateInputBuilders: Boolean?,
    override val nullableFieldStyle: String?,
    override val decapitalizeFields: Boolean?,
    override val jsExport: Boolean?,
    override val addTypename: String?,
    override val flattenModels: Boolean?,
    override val fieldsOnDisjointTypesMustMerge: Boolean?,
    override val generateApolloMetadata: Boolean?,
    override val usedOptions: Set<String>,
) : ApolloGradleToolingModel.TelemetryData.ServiceTelemetryData, Serializable
