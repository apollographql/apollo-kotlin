package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException

/**
 * An [ApolloAdapter] is responsible for adapting Kotlin-generated GraphQL types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - serialize variables
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * This class is implemented by the generated code, it shouldn't be used directly.
 */
interface ApolloAdapter<T> {
  @Throws(IOException::class)
  fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): T

  @Throws(IOException::class)
  fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T)
}
