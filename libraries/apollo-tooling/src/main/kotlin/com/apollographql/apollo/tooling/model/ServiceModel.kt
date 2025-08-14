package com.apollographql.apollo.tooling.model

import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloExperimental
@Serializable
class ServiceModel(
    /**
     * The Gradle project path of the service, e.g. ":feature:myFeature1"
     */
    val gradleProjectPath: String,
    val serviceName: String,
    val schemaFiles: Set<String>,
    val graphqlSrcDirs: Set<String>,

    /**
     * Upstream Gradle project paths e.g. [":graphqlShared"]
     */
    val upstreamGradleProjectPaths: Set<String>,

    /**
     * Downstream Gradle project paths e.g. [":feature:myFeature2"]
     */
    val downstreamGradleProjectPaths: Set<String>,
    val endpointUrl: String?,
    val endpointHeaders: Map<String, String>?,

    val telemetryUsedOptions: Set<String>,
)

@ApolloExperimental
fun ServiceModel.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloExperimental
fun File.toServiceModel(): ServiceModel {
  @Suppress("JSON_FORMAT_REDUNDANT")
  return Json { ignoreUnknownKeys = true }.decodeFromString(readText())
}
