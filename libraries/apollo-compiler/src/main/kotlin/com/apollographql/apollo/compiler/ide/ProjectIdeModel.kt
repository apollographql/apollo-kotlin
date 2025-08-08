package com.apollographql.apollo.compiler.ide

import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@ApolloInternal
@Serializable
class ProjectIdeModel(
    val serviceNames: Set<String>,
)

@ApolloInternal
fun ProjectIdeModel.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

@ApolloInternal
fun File.toProjectIdeModel(): ProjectIdeModel {
  return Json.decodeFromString(readText())
}
