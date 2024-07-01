package com.apollographql.apollo.compiler.pqm

import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import kotlinx.serialization.Serializable

@Serializable
class PersistedQueryManifest(
    val format: String,
    val version: Int,
    val operations: List<PqmOperation>
)

@Serializable
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