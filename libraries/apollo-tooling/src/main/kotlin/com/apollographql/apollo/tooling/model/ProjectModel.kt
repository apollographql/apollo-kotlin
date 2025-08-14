package com.apollographql.apollo.tooling.model

import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloExperimental
@Serializable
class ProjectModel(
    val serviceNames: Set<String>,
    val telemetryData: TelemetryData,
) {
  @Serializable
  class TelemetryData(
      val gradleVersion: String?,
      val androidMinSdk: Int?,
      val androidTargetSdk: Int?,
      val androidCompileSdk: String?,
      val androidAgpVersion: String?,
      val apolloGenerateSourcesDuringGradleSync: Boolean?,
      val apolloLinkSqlite: Boolean?,
  )
}

@ApolloExperimental
fun ProjectModel.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloExperimental
fun File.toProjectModel(): ProjectModel {
  @Suppress("JSON_FORMAT_REDUNDANT")
  return Json { ignoreUnknownKeys = true }.decodeFromString(readText())
}
