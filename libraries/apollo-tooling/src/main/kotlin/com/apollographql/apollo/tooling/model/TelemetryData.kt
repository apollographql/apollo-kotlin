package com.apollographql.apollo.tooling.model

import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloInternal
@Serializable
class TelemetryData(
    val gradleVersion: String?,
    val androidMinSdk: Int?,
    val androidTargetSdk: Int?,
    val androidCompileSdk: String?,
    val androidAgpVersion: String?,
    val apolloGenerateSourcesDuringGradleSync: Boolean?,
    val apolloLinkSqlite: Boolean?,

    val usedServiceOptions: Set<String>,
)

@ApolloInternal
fun TelemetryData.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloInternal
fun File.toTelemetryData(): TelemetryData {
  @Suppress("JSON_FORMAT_REDUNDANT")
  return Json { ignoreUnknownKeys = true }.decodeFromString(readText())
}
