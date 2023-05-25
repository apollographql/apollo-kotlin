package com.apollographql.apollo3.compiler.pqm

import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PersistedQueryManifest(
    val format: String,
    val version: Int,
    val operations: List<PqmOperation>
)

@JsonClass(generateAdapter = true)
class PqmOperation(
    val id: String,
    val body: String,
    val name: String,
    val type: String,
)

fun OperationOutput.toPersistedQueryManifest(): PersistedQueryManifest {
  return PersistedQueryManifest(
      format = "apollo-persisted-query-manifest",
      version = 1,
      operations = entries.map {
        PqmOperation(
            id = it.key,
            body = it.value.source,
            name = it.value.name,
            type = it.value.type
        )
      }
  )
}