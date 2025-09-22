package com.apollographql.apollo.compiler.model

import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloInternal
@Serializable
class ProjectModel(
    val serviceNames: Set<String>,
    /**
     * Absolute paths to the Apollo Gradle task dependencies.
     */
    val apolloTasksDependencies: Set<String>,
)

@ApolloInternal
fun ProjectModel.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloInternal
fun File.toProjectModel(): ProjectModel {
  @Suppress("JSON_FORMAT_REDUNDANT")
  return Json { ignoreUnknownKeys = true }.decodeFromString(readText())
}
