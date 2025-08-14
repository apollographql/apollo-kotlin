package com.apollographql.apollo.compiler.model

import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloInternal
@Serializable
class CompilationUnitModel(
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

    /**
     * Absolute paths to Apollo Compiler Plugin dependencies.
     */
    val pluginDependencies: Set<String>,
)

@ApolloInternal
fun CompilationUnitModel.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloInternal
fun File.toCompilationUnitModel(): CompilationUnitModel {
  @Suppress("JSON_FORMAT_REDUNDANT")
  return Json { ignoreUnknownKeys = true }.decodeFromString(readText())
}
