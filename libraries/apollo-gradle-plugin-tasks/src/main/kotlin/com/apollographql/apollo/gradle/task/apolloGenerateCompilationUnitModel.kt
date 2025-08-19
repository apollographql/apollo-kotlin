package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.model.CompilationUnitModel
import com.apollographql.apollo.compiler.model.writeTo
import gratatouille.tasks.GAny
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@GTask
internal fun apolloGenerateCompilationUnitModel(
    // Inputs
    gradleProjectPath: String,
    serviceName: String,
    schemaFiles: Set<String>,
    graphqlSrcDirs: Set<String>,
    upstreamGradleProjectPaths: Set<String>,
    downstreamGradleProjectPaths: Set<String>,
    endpointUrl: String?,
    endpointHeaders: Map<String, String>?,
    pluginDependencies: Set<String>,
    pluginArguments: Map<String, GAny?>,

    // Outputs
    compilationUnitModel: GOutputFile,
) {
  CompilationUnitModel(
      gradleProjectPath = gradleProjectPath,
      serviceName = serviceName,
      schemaFiles = schemaFiles,
      graphqlSrcDirs = graphqlSrcDirs,
      upstreamGradleProjectPaths = upstreamGradleProjectPaths,
      downstreamGradleProjectPaths = downstreamGradleProjectPaths,
      endpointUrl = endpointUrl,
      endpointHeaders = endpointHeaders,
      pluginDependencies = pluginDependencies,
      pluginArguments = pluginArguments.mapValues { (_, v) -> v.toJsonElement() },
  )
      .writeTo(compilationUnitModel)
}

@Suppress("UNCHECKED_CAST")
private fun Any?.toJsonElement(): JsonElement = when (this) {
  is Map<*, *> -> JsonObject(mapValues { it.value.toJsonElement() } as Map<String, JsonElement>)
  is List<*> -> JsonArray(map { it.toJsonElement() })
  is Boolean -> JsonPrimitive(this)
  is Number -> JsonPrimitive(this)
  is String -> JsonPrimitive(this)
  null -> JsonNull
  else -> error("cannot convert $this to JsonElement")
}
