package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

/**
 * A [ResponseAdapter] is responsible for adapting GraphQL types to Kotlin types.
 *
 * It is used to
 * - deserialize network responses
 * - serialize variables
 * - normalize models into records that can be stored in cache
 * - deserialize records
 */
interface ResponseAdapter<T> {
  fun fromResponse(reader: JsonReader): T
  fun toResponse(writer: JsonWriter, value: T)
}
