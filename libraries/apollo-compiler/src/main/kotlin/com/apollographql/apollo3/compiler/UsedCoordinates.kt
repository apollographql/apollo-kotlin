package com.apollographql.apollo3.compiler

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.buffer
import okio.sink
import okio.source
import okio.use
import java.io.File

typealias UsedCoordinates = Map<String, Set<String>>

fun UsedCoordinates.mergeWith(other: UsedCoordinates): UsedCoordinates {
  return (entries + other.entries).groupBy { it.key }.mapValues {
    it.value.map { it.value }.fold(emptySet()) { acc, set ->
      (acc + set).toSet()
    }
  }
}

/**
 * An intermediate structure that we used for serialization to enforce the order of elements in the Set.
 * I'm not 100% convinced it's required but it doesn't harm and could avoid nasty build cache issues.
 */
@Serializable
private class SerializableUsedCoordinates(
    val usedFields: Map<String, List<String>>,
)

private fun UsedCoordinates.toSerializableUsedCoordinates(): SerializableUsedCoordinates = SerializableUsedCoordinates(
    usedFields = mapValues { it.value.toList().sorted() }
)

private fun SerializableUsedCoordinates.toUsedCoordinates(): UsedCoordinates = usedFields.mapValues { it.value.toSet() }

@OptIn(ExperimentalSerializationApi::class)
fun UsedCoordinates.writeTo(file: File) {
  file.sink().buffer().use {
    Json.encodeToBufferedSink(this@writeTo.toSerializableUsedCoordinates(), it)
  }
}

@OptIn(ExperimentalSerializationApi::class)
fun File.toUsedCoordinates(): UsedCoordinates {
  return this.source().buffer().use {
    Json.decodeFromBufferedSource<SerializableUsedCoordinates>(it).toUsedCoordinates()
  }
}