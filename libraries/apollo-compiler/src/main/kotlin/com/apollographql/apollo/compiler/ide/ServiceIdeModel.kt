package com.apollographql.apollo.compiler.ide

import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloInternal
@Serializable
class ServiceIdeModel(
    val projectPath: String,
    val serviceName: String,
    val schemaFiles: Set<String>,
    val graphqlSrcDirs: Set<String>,
    val upstreamProjectPaths: Set<String>,
    val downstreamProjectPaths: Set<String>,
    val endpointUrl: String?,
    val endpointHeaders: Map<String, String>?,
    val useSemanticNaming: Boolean,
)

@ApolloInternal
fun ServiceIdeModel.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloInternal
fun File.toServiceIdeModel(): ServiceIdeModel {
  return Json.decodeFromString(readText())
}
