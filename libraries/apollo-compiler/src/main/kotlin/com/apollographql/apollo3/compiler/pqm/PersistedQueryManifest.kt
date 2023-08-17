package com.apollographql.apollo3.compiler.pqm

import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

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

@OptIn(ExperimentalSerializationApi::class)
fun PersistedQueryManifest.writeTo(file: File) {
  file.sink().buffer().use {
    Json.encodeToBufferedSink<PersistedQueryManifest>(this, it)
  }
}

@OptIn(ExperimentalSerializationApi::class)
fun File.toPersistedQueryManifest(): PersistedQueryManifest {
  return source().buffer().use {
    Json.decodeFromBufferedSource(it)
  }
}

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